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

package com.arm.peliondevicemanagement.services.store

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.DATABASE_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.AppConstants.NETWORK_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.constants.state.workflow.WorkflowState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils.downloadTaskAssets
import com.arm.peliondevicemanagement.utils.WorkflowUtils.fetchSDAToken
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getAudienceListFromDevices
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getPermissionScopeFromTasks
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isValidSDAToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PendingWorkflowDataSource(
    private val scope: CoroutineScope,
    private val cloudRepository: CloudRepository,
    private val localCache: LocalCache,
    private val stateLiveData: MutableLiveData<LoadState>
): PageKeyedDataSource<String, Workflow>()  {

    companion object {
        private val TAG: String = PendingWorkflowDataSource::class.java.simpleName
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, Workflow>
    ) {
        var workflowList: List<Workflow>

        if(WorkflowViewModel.isNetworkFetchMandatory){
            LogHelper.debug(TAG, "Deleting local-cache, performing full network-refresh")
            WorkflowUtils.deleteWorkflowsCache {
                scope.launch {
                    requestAndSaveData {
                        workflowList = localCache
                            .fetchWorkflowsByMultiStatus(DATABASE_PAGE_SIZE,
                                WorkflowState.PENDING.name, WorkflowState.SYNCED.name)

                        if(workflowList.isNotEmpty()){
                            workflowList = processSDATokenValidity(workflowList)
                            val lastItem = workflowList.last()
                            LogHelper.debug(TAG, "loadInitial() loadedItems: ${workflowList.size}, " +
                                    "afterID: ${lastItem.workflowID}")
                            callback.onResult(workflowList, null, lastItem.workflowID)
                        } else {
                            LogHelper.debug(TAG, "loadInitial() No more items available")
                            callback.onResult(listOf(), null, null)
                        }

                    }
                }
            }
        } else {
            workflowList = localCache
                .fetchWorkflowsByMultiStatus(DATABASE_PAGE_SIZE,
                    WorkflowState.PENDING.name, WorkflowState.SYNCED.name)

            if(workflowList.isEmpty()){
                // If local-cache doesn't have this then fetch from the network
                LogHelper.debug(TAG, "LocalCache not-found, making network-request")
                scope.launch {
                    requestAndSaveData {
                        workflowList = localCache
                            .fetchWorkflowsByMultiStatus(DATABASE_PAGE_SIZE,
                                WorkflowState.PENDING.name, WorkflowState.SYNCED.name)

                        if(workflowList.isNotEmpty()){
                            workflowList = processSDATokenValidity(workflowList)
                            val lastItem = workflowList.last()
                            LogHelper.debug(TAG, "loadInitial() loadedItems: ${workflowList.size}, " +
                                    "afterID: ${lastItem.workflowID}")
                            callback.onResult(workflowList, null, lastItem.workflowID)
                        } else {
                            LogHelper.debug(TAG, "loadInitial() No more items available")
                            callback.onResult(listOf(), null, null)
                        }

                    }
                }
            } else {
                workflowList = processSDATokenValidity(workflowList)
                val lastItem = workflowList.last()
                LogHelper.debug(TAG, "loadInitial() loadedItems: ${workflowList.size}, " +
                        "afterID: ${lastItem.workflowID}")
                callback.onResult(workflowList, null, lastItem.workflowID)
            }
        }
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, Workflow>
    ) {
        // params.key = afterID
        LogHelper.debug(TAG, "loadAfter() afterID: ${params.key}")
        var workflowList: List<Workflow>
        workflowList = localCache
            .fetchWorkflowsByMultiStatus(DATABASE_PAGE_SIZE,
                WorkflowState.PENDING.name, WorkflowState.SYNCED.name, params.key)

        if(workflowList.isEmpty()){
            // If local-cache doesn't have this then fetch from the network
            LogHelper.debug(TAG, "LocalCache not-found, making network-request")
            scope.launch {
                requestAndSaveData(params.key) {
                    workflowList = localCache
                        .fetchWorkflowsByMultiStatus(DATABASE_PAGE_SIZE,
                            WorkflowState.PENDING.name, WorkflowState.SYNCED.name, params.key)

                    if(workflowList.isNotEmpty()){
                        workflowList = processSDATokenValidity(workflowList)
                        val lastItem = workflowList.last()
                        LogHelper.debug(TAG, "loadAfter() loadedItems: ${workflowList.size}, " +
                                "afterID: ${lastItem.workflowID}")
                        callback.onResult(workflowList, lastItem.workflowID)
                    } else {
                        LogHelper.debug(TAG, "loadAfter() No more items available")
                        callback.onResult(listOf(), params.key)
                    }
                }
            }
        } else {
            workflowList = processSDATokenValidity(workflowList)
            val lastItem = workflowList.last()
            LogHelper.debug(TAG, "loadAfter() loadedItems: ${workflowList.size}, " +
                    "afterID: ${lastItem.workflowID}")
            callback.onResult(workflowList, lastItem.workflowID)
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, Workflow>
    ) {
        // Not needed
    }

    private suspend fun requestAndSaveData(after: String? = null, saveFinished: () -> Unit) {
        //val status = WorkflowState.PENDING.name
        val assigneeID = SharedPrefHelper.getSelectedUserID()!!
        try {
            val response = if(after != null){
                cloudRepository.getAssignedWorkflows(NETWORK_PAGE_SIZE, assigneeID, after)
            } else {
                cloudRepository.getAssignedWorkflows(NETWORK_PAGE_SIZE, assigneeID)
            }

            if(response?.workflows!!.isNotEmpty()){
                stateLiveData.postValue(LoadState.DOWNLOADING)
                processWorkflowsData(response.workflows){
                    saveFinished()
                }
            } else {
                LogHelper.debug(TAG, "Network data-unavailable")
                stateLiveData.postValue(LoadState.FAILED)
                saveFinished()
            }
        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
            if(errorResponse != null){
                stateLiveData.postValue(LoadState.UNAUTHORIZED)
            } else {
                stateLiveData.postValue(LoadState.NO_NETWORK)
            }
            saveFinished()
        }
    }

    private suspend fun processWorkflowsData(workflows: List<Workflow>, saveFinished: () -> Unit) {
        val processedWorkflows = arrayListOf<Workflow>()
        workflows.forEach { workflow ->
            val isStored = localCache.isWorkflowStored(workflow.workflowID)
            if(!isStored && (workflow.workflowStatus == WorkflowState.SYNCED.name
                        || workflow.workflowStatus == WorkflowState.PENDING.name)){
                // Store accountID
                workflow.accountID = SharedPrefHelper.getSelectedAccountID()
                // Parse AUDs into devices
                workflow.workflowDevices = arrayListOf()
                workflow.workflowAUDs.forEach { aud ->
                    workflow.workflowDevices!!.add(
                        WorkflowDevice(
                            aud.substring(3, aud.length),
                            AppConstants.DEVICE_STATE_PENDING
                        )
                    )
                }

                // Fetch SDA_token
                // Enable feature-flag, if debug-build
                if(BuildConfig.DEBUG){
                    if(!SharedPrefHelper.getDeveloperOptions().isSDATokenDownloadDisabled()){
                        val sdaTokenResponse = fetchAndSaveSDAToken(workflow)
                        if(sdaTokenResponse != null){
                            workflow.sdaToken = sdaTokenResponse
                        }
                    }
                } else {
                    val sdaTokenResponse = fetchAndSaveSDAToken(workflow)
                    if(sdaTokenResponse != null){
                        workflow.sdaToken = sdaTokenResponse
                    }
                }

                // Download Task Assets
                // Enable feature-flag, if debug-build
                if(BuildConfig.DEBUG){
                    if(!SharedPrefHelper.getDeveloperOptions().isAssetDownloadDisabled()){
                        downloadTaskAssets(cloudRepository,
                            workflow.workflowID, workflow.workflowTasks)
                    }
                } else {
                    downloadTaskAssets(cloudRepository,
                        workflow.workflowID, workflow.workflowTasks)
                }

                // Sync workflow
                // Enable feature-flag, if debug-build
                if(BuildConfig.DEBUG) {
                    if(!SharedPrefHelper.getDeveloperOptions().isJobAutoSyncDisabled()){
                        // Sync-job
                        val isSyncSuccessful = cloudRepository.syncWorkflow(workflow.workflowID)
                        if(isSyncSuccessful){
                            workflow.workflowStatus = WorkflowState.SYNCED.name
                            LogHelper.debug(TAG, "Workflow with ID: ${workflow.workflowID} synced")
                        } else {
                            LogHelper.debug(TAG, "Workflow with ID: ${workflow.workflowID} sync-failed")
                        }
                    }
                } else {
                    // Sync-job
                    val isSyncSuccessful = cloudRepository.syncWorkflow(workflow.workflowID)
                    if(isSyncSuccessful){
                        workflow.workflowStatus = WorkflowState.SYNCED.name
                        LogHelper.debug(TAG, "Workflow with ID: ${workflow.workflowID} synced")
                    } else {
                        LogHelper.debug(TAG, "Workflow with ID: ${workflow.workflowID} sync-failed")
                    }
                }

                // Now process workflow for local-caching
                processedWorkflows.add(workflow)
            } else {
                LogHelper.debug(TAG, "Workflow with ID: ${workflow.workflowID} already stored, skipping")
            }
        }

        if(processedWorkflows.isNotEmpty()){
            // Store in DB
            localCache.insertWorkflows(processedWorkflows) {
                LogHelper.debug(TAG, "LocalCache->Success()")
                stateLiveData.postValue(LoadState.DOWNLOADED)
                saveFinished()
            }
        } else {
            LogHelper.debug(TAG, "LocalCache->Skipped()")
            saveFinished()
        }
    }

    private suspend fun fetchAndSaveSDAToken(workflow: Workflow): SDATokenResponse? {
        // Fetch permission-scope
        val permissionScope = getPermissionScopeFromTasks(workflow.workflowTasks)
        // Fetch audience
        val audienceList = getAudienceListFromDevices(workflow.workflowDevices!!)
        // Call access-token request
        return fetchSDAToken(cloudRepository, permissionScope, audienceList)
    }

    private fun processSDATokenValidity(workflowList: List<Workflow>): List<Workflow> {
        LogHelper.debug(TAG, "processSDATokenValidity() processing token-validity")
        var validCount = 0
        workflowList.forEach { workflow ->
            if(workflow.sdaToken != null) {
                if(isValidSDAToken(workflow.sdaToken!!.expiresIn)){
                    // Set validity
                    workflow.sdaToken!!.isValid = true
                    // Process readable date-time
                    val expiresIn = workflow.sdaToken!!.expiresIn
                    val expiryDate = PlatformUtils.parseJSONTimeString(expiresIn)
                    val expiryTime =
                        PlatformUtils.parseJSONTimeString(expiresIn, AppConstants.DEFAULT_TIME_FORMAT)
                    val expiryDateTime = "$expiryDate, $expiryTime"
                    workflow.sdaToken!!.readableDateTime = expiryDateTime
                    validCount++
                } else {
                    workflow.sdaToken!!.isValid = false
                }
            }
        }
        LogHelper.debug(TAG, "processSDATokenValidity() found $validCount workflow with valid-token")
        return workflowList
    }
}