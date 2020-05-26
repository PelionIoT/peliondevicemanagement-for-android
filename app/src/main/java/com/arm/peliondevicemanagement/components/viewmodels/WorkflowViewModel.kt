/*
 * Copyright 2020 ARM Ltd.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.components.models.workflow.task.WorkflowTask
import com.arm.peliondevicemanagement.constants.APIConstants.CONTENT_TYPE_TEXT_PLAIN
import com.arm.peliondevicemanagement.constants.AppConstants.TASK_NAME_FILE
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.constants.state.workflow.task.TaskRunState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.repository.LocalRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.cache.WorkflowDB
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.peliondevicemanagement.services.data.FileUploadResponse
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils.convertDeviceRunLogsToJson
import com.arm.peliondevicemanagement.utils.WorkflowUtils.downloadTaskAssets
import com.arm.peliondevicemanagement.utils.WorkflowUtils.fetchSDAToken
import com.arm.peliondevicemanagement.utils.WorkflowUtils.fetchTaskOutputAsset
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getWorkflowTaskIDs
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isWorkflowAssetsDownloaded
import com.arm.peliondevicemanagement.utils.WorkflowUtils.saveWorkflowTaskOutputAsset
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class WorkflowViewModel : ViewModel() {

    companion object {
        private val TAG: String = WorkflowViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)

    private val cloudRepository: CloudRepository = AppController.getCloudRepository()
    private var localRepository: LocalRepository
    private val workflowDB: WorkflowDB = AppController.getWorkflowDB()
    private var localCache: LocalCache

    private lateinit var _pendingWorkflowsLiveData: LiveData<PagedList<Workflow>>
    private lateinit var _completedWorkflowsLiveData: LiveData<PagedList<Workflow>>
    private val _refreshStateLiveData = MutableLiveData<LoadState>()
    private val _refreshedSDATokenLiveData = MutableLiveData<SDATokenResponse>()
    private val _assetAvailableLiveData = MutableLiveData<Boolean>()
    private val _workflowSyncStateLiveData = MutableLiveData<Boolean>()
    private val _assetUploadResponseLiveData = MutableLiveData<Int>()
    private val _errorResponseLiveData = MutableLiveData<ErrorResponse>()

    private val boundaryCallback = object: PagedList.BoundaryCallback<Workflow>() {
        override fun onZeroItemsLoaded() {
            super.onZeroItemsLoaded()
            // Handle empty initial load here
            LogHelper.debug(TAG, "BoundaryCallback()->onZeroItemsLoaded")
            _refreshStateLiveData.postValue(LoadState.EMPTY)
        }

        override fun onItemAtEndLoaded(itemAtEnd: Workflow) {
            super.onItemAtEndLoaded(itemAtEnd)
            // Here you can listen to last item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtEndLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }

        override fun onItemAtFrontLoaded(itemAtFront: Workflow) {
            super.onItemAtFrontLoaded(itemAtFront)
            // Here you can listen to first item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtFrontLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }
    }

    init {
        localCache = LocalCache(workflowDB.workflowsDao(), Executors.newSingleThreadExecutor())
        localRepository =
            LocalRepository(
                scope,
                cloudRepository,
                localCache
            )
    }

    fun initPendingWorkflowLiveData() {
        _pendingWorkflowsLiveData = getPendingWorkflowsLiveData()
    }

    fun initCompletedWorkflowsLiveData() {
        _completedWorkflowsLiveData = getCompletedWorkflowsLiveData()
    }

    fun getPendingWorkflows(): LiveData<PagedList<Workflow>> = _pendingWorkflowsLiveData
    fun getCompletedWorkflows(): LiveData<PagedList<Workflow>> = _completedWorkflowsLiveData
    fun getRefreshState(): LiveData<LoadState> = _refreshStateLiveData
    fun getAssetAvailabilityStatus(): LiveData<Boolean> = _assetAvailableLiveData
    fun getWorkflowSyncState(): LiveData<Boolean> = _workflowSyncStateLiveData
    fun getAssetUploadLiveData(): LiveData<Int> = _assetUploadResponseLiveData
    fun getErrorResponseLiveData(): LiveData<ErrorResponse> = _errorResponseLiveData

    fun refreshPendingWorkflows() {
        _pendingWorkflowsLiveData.value?.dataSource?.invalidate()
    }

    fun refreshCompletedWorkflows() {
        _completedWorkflowsLiveData.value?.dataSource?.invalidate()
    }

    fun refreshSDAToken(permissionScope: String, audienceList: List<String>) {
        scope.launch {
            try {
                val popPemPubKey = WorkflowUtils.getSDAPopPemPubKey()
                val request = WorkflowUtils.createSDATokenRequest(popPemPubKey, permissionScope, audienceList)
                //LogHelper.debug(TAG, "SDA_Token_Request-> $request")
                val tokenResponse = cloudRepository.getSDAToken(request)
                WorkflowUtils.validateSDATokenSanity(tokenResponse?.accessToken!!, popPemPubKey)
                //LogHelper.debug(TAG, "TokenSanity->Passed")
                _refreshedSDATokenLiveData.postValue(tokenResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun updateLocalSDAToken(workflowID: String, sdaToken: SDATokenResponse?) {
        localCache.updateSDAToken(workflowID, sdaToken) {
            LogHelper.debug(TAG, "SDA-Token updated.")
        }
    }

    fun updateWorkflowStatus(workflowID: String, workflowStatus: String) {
        localCache.updateWorkflowStatus(workflowID, workflowStatus) {
            LogHelper.debug(TAG, "Workflow status updated.")
        }
    }

    fun fetchSingleWorkflow(workflowID: String, fetched: (workflow: Workflow) -> Unit) {
        localCache.fetchSingleWorkflow(workflowID){ workflow ->
            fetched(workflow)
        }
    }

    fun updateWorkflowDevices(workflowID: String, devices: ArrayList<WorkflowDevice>, updateFinished: () -> Unit) {
        localCache.updateWorkflowDevices(workflowID, devices) {
            updateFinished()
        }
    }

    fun saveWorkflowTaskOutputAssets(workflowID: String, taskID: String, fileContent: ByteArray) {
        saveWorkflowTaskOutputAsset(workflowID, taskID, fileContent){ success ->
            if(success){
                LogHelper.debug(TAG, "Output asset saved for task: $taskID")
            } else {
                LogHelper.debug(TAG, "Failed to save output asset for task: $taskID")
            }
        }
    }

    fun getRefreshedSDAToken(): LiveData<SDATokenResponse> = _refreshedSDATokenLiveData

    private fun getPendingWorkflowsLiveData(): LiveData<PagedList<Workflow>> {
        val pageConfig = PagedList.Config.Builder()
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setEnablePlaceholders(false)
            .build()

        // Fetch data from the local-cache and return a factory
        val dataSourceFactory = localRepository.fetchPendingWorkflowsFactory(_refreshStateLiveData)

        // Get the paged list
        return LivePagedListBuilder(dataSourceFactory, pageConfig)
            .setBoundaryCallback(boundaryCallback)
            .build()
    }

    private fun getCompletedWorkflowsLiveData(): LiveData<PagedList<Workflow>> {
        val pageConfig = PagedList.Config.Builder()
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setEnablePlaceholders(false)
            .build()

        // Fetch data from the local-cache and return a factory
        val dataSourceFactory = localRepository
            .fetchCompletedWorkflowsFactory()

        // Get the paged list
        return LivePagedListBuilder(dataSourceFactory, pageConfig)
            .setBoundaryCallback(boundaryCallback)
            .build()
    }

    fun checkForWorkflowAssets(workflowID: String, workflowTasks: List<WorkflowTask>) {
        scope.launch {
            val status = isWorkflowAssetsDownloaded(workflowID, workflowTasks)
            _assetAvailableLiveData.postValue(status)
        }
    }

    fun downloadWorkflowAssets(workflowID: String, workflowTasks: List<WorkflowTask>) {
        scope.launch {
            try {
                downloadTaskAssets(cloudRepository, workflowID, workflowTasks)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "${e.message}")
                val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
            checkForWorkflowAssets(workflowID, workflowTasks)
        }
    }

    fun syncWorkflow(workflowID: String) {
        scope.launch {
            try {
                val isSuccess = cloudRepository.syncWorkflow(workflowID)
                if(isSuccess){
                    _workflowSyncStateLiveData.postValue(true)
                } else {
                    _workflowSyncStateLiveData.postValue(false)
                }
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "${e.message}")
                val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    private suspend fun uploadWorkflowTaskAssetFile(workflowID: String, taskID: String): FileUploadResponse? {
        try {
            val fileForUpload = fetchTaskOutputAsset(workflowID, taskID)
            return if(fileForUpload != null) {
                val requestBody = fileForUpload
                    .asRequestBody(CONTENT_TYPE_TEXT_PLAIN)

                val filePart = MultipartBody.Part
                    .createFormData(TASK_NAME_FILE,
                        fileForUpload.name, requestBody)

                LogHelper.debug(TAG, "Uploading task asset with taskID: $taskID")
                cloudRepository.uploadWorkflowTaskAssetFile(filePart)
            } else {
                null
            }
        } catch (e: Throwable){
            LogHelper.debug(TAG, "${e.message}")
            return null
        }
    }

    fun processWorkflowsForTaskAssetUpload(workflows: List<Workflow>) {
        scope.launch {
            try {
                var totalUploadCount = (workflows.size - 1)
                workflows.forEach { workflow ->
                    val listOfUploadResponse = hashMapOf<String, FileUploadResponse>()
                    // Fetch task-IDs for READ-Tasks
                    val listOfTaskIDs = getWorkflowTaskIDs(workflow.workflowTasks)
                    // Now process, asset upload
                    if(listOfTaskIDs.isNotEmpty()){
                        LogHelper.debug(TAG, "Found ${listOfTaskIDs.size} asset for upload, processing")

                        listOfTaskIDs.forEach { taskID ->
                            val uploadResponse = uploadWorkflowTaskAssetFile(workflow.workflowID, taskID)
                            if(uploadResponse != null){
                                listOfUploadResponse[taskID] = uploadResponse
                                LogHelper.debug(TAG, "Upload complete of asset with taskID: $taskID, " +
                                        "fileID: ${uploadResponse.fileID}, size: ${uploadResponse.fileSize}")
                            }
                        }

                        LogHelper.debug(TAG, "Found ${listOfUploadResponse.size} upload response")

                        if(listOfUploadResponse.isNotEmpty()){
                            LogHelper.debug(TAG, "Updating ${workflow.workflowDevices?.size} device of workflowID: ${workflow.workflowID}")
                            workflow.workflowDevices?.forEach { device ->
                                LogHelper.debug(TAG, "Updating device-run-logs of device: ${device.deviceName}")
                                device.deviceRunLogs?.deviceTaskRuns?.forEach { taskRun->
                                    if(listOfUploadResponse.containsKey(taskRun.taskID)
                                        && taskRun.taskStatus == TaskRunState.SUCCEEDED.name){
                                        LogHelper.debug(TAG, "Updating task-run-logs of taskID: ${taskRun.taskID}")
                                        taskRun.outputParameters!![1].paramValue = listOfUploadResponse[taskRun.taskID]!!.fileID
                                    }
                                }
                            }

                            // Update local-cache of workflow
                            updateWorkflowDevices(workflow.workflowID, workflow.workflowDevices!!){
                                LogHelper.debug(TAG, "Devices updated in local-cache.")
                            }
                        }
                    }
                    // Now process, run-logs
                    LogHelper.debug(TAG, "->Move to device-run log upload")
                    workflow.workflowDevices?.forEach { device ->
                        val runLog = convertDeviceRunLogsToJson(device.deviceRunLogs!!)
                        LogHelper.debug(TAG, "Device-Logs: $runLog")
                        LogHelper.debug(TAG, "Uploading logs for deviceID: ${device.deviceName}")
                        val response = cloudRepository.uploadDeviceRunLogs(runLog)
                        if(response != null){
                            LogHelper.debug(TAG, "Logs uploaded for deviceID: ${device.deviceName}, " +
                                    "logs: $response")
                        }
                    }
                    localCache.updateWorkflowUploadStatus(workflow.workflowID, true){
                        LogHelper.debug(TAG, "Workflow assets & logs uploaded successfully")
                        _assetUploadResponseLiveData.postValue(totalUploadCount--)
                    }
                }
            } catch (e: Exception){
                LogHelper.debug(TAG, "${e.message}")
                _assetUploadResponseLiveData.postValue(-1)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}