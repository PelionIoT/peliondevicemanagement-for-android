/*
 * Copyright (c) 2018, Arm Limited and affiliates.
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
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.LocalRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.cache.WorkflowDB
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.utils.WorkflowUtils.downloadTaskAssets
import com.arm.peliondevicemanagement.utils.WorkflowUtils.fetchSDAToken
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isWorkflowAssetsDownloaded
import com.arm.peliondevicemanagement.utils.WorkflowUtils.saveWorkflowTaskOutputAsset
import kotlinx.coroutines.*
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
    private val _pendingWorkflowLiveData = MutableLiveData<Workflow>()
    private val _assetAvailableLiveData = MutableLiveData<Boolean>()

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
        localRepository = LocalRepository(scope, cloudRepository, localCache)
    }

    fun initPendingWorkflowLiveData() {
        _pendingWorkflowsLiveData = getPendingWorkflowsLiveData()
    }

    fun initCompletedWorkflowsLiveData() {
        _completedWorkflowsLiveData = getCompletedWorkflowsLiveData()
    }

    fun getPendingWorkflows(): LiveData<PagedList<Workflow>> = _pendingWorkflowsLiveData

    fun getCompletedWorkflows(): LiveData<PagedList<Workflow>> = _completedWorkflowsLiveData

    fun getWorkflow(): LiveData<Workflow> = _pendingWorkflowLiveData

    fun getRefreshState(): LiveData<LoadState> = _refreshStateLiveData

    fun getAssetAvailabilityStatus(): LiveData<Boolean> = _assetAvailableLiveData

    fun refreshPendingWorkflows() {
        _pendingWorkflowsLiveData.value?.dataSource?.invalidate()
    }

    fun refreshCompletedWorkflows() {
        _completedWorkflowsLiveData.value?.dataSource?.invalidate()
    }

    fun refreshSDAToken(permissionScope: String, audienceList: List<String>) {
        scope.launch {
            val tokenResponse = fetchSDAToken(
                cloudRepository,
                permissionScope,
                audienceList)
            if(tokenResponse != null){
                _refreshedSDATokenLiveData.postValue(tokenResponse)
            } else {
                _refreshedSDATokenLiveData.postValue(null)
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

    fun fetchWorkflow(workflowID: String) {
        scope.launch {
            val workflow = localCache.fetchSingleWorkflow(workflowID)
            _pendingWorkflowLiveData.postValue(workflow)
        }
    }

    fun updateWorkflowDevices(workflowID: String, devices: ArrayList<WorkflowDevice>) {
        localCache.updateWorkflowDevices(workflowID, devices) {
            LogHelper.debug(TAG, "Devices updated.")
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
            .fetchCompletedWorkflowsFactory(_refreshStateLiveData)

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
            downloadTaskAssets(cloudRepository, workflowID, workflowTasks)
            checkForWorkflowAssets(workflowID, workflowTasks)
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}