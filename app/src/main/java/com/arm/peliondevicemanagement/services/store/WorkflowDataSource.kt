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

package com.arm.peliondevicemanagement.services.store

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.DATABASE_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.AppConstants.NETWORK_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.utils.WorkflowUtils.downloadTaskAssets
import com.arm.peliondevicemanagement.utils.WorkflowUtils.fetchSDAToken
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getAudienceListFromDevices
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getPermissionScopeFromTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WorkflowDataSource(
    private val scope: CoroutineScope,
    private val cloudRepository: CloudRepository,
    private val localCache: LocalCache,
    private val stateLiveData: MutableLiveData<LoadState>
): PageKeyedDataSource<String, WorkflowModel>()  {

    companion object {
        private val TAG: String = WorkflowDataSource::class.java.simpleName
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, WorkflowModel>
    ) {
        var workflowList: List<WorkflowModel>
        workflowList = localCache.fetchWorkflows(DATABASE_PAGE_SIZE)

        if(workflowList.isEmpty()){
            // If local-cache doesn't have this then fetch from the network
            LogHelper.debug(TAG, "LocalCache not-found, making network-request")
            scope.launch {
                requestAndSaveData {
                    workflowList = localCache.fetchWorkflows(DATABASE_PAGE_SIZE)
                    if(workflowList.isNotEmpty()){
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
            val lastItem = workflowList.last()
            LogHelper.debug(TAG, "loadInitial() loadedItems: ${workflowList.size}, " +
                    "afterID: ${lastItem.workflowID}")
            callback.onResult(workflowList, null, lastItem.workflowID)
        }


    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, WorkflowModel>
    ) {
        // params.key = afterID
        LogHelper.debug(TAG, "loadAfter() afterID: ${params.key}")
        var workflowList: List<WorkflowModel>
        workflowList = localCache.fetchWorkflows(DATABASE_PAGE_SIZE, params.key)

        if(workflowList.isEmpty()){
            // If local-cache doesn't have this then fetch from the network
            LogHelper.debug(TAG, "LocalCache not-found, making network-request")
            scope.launch {
                requestAndSaveData(params.key) {
                    workflowList = localCache.fetchWorkflows(DATABASE_PAGE_SIZE, params.key)
                    if(workflowList.isNotEmpty()){
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
            val lastItem = workflowList.last()
            LogHelper.debug(TAG, "loadAfter() loadedItems: ${workflowList.size}, " +
                    "afterID: ${lastItem.workflowID}")
            callback.onResult(workflowList, lastItem.workflowID)
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, WorkflowModel>
    ) {
        // Not needed
    }

    private suspend fun requestAndSaveData(after: String? = null, saveFinished: () -> Unit) {

        try {
            val response = if(after != null){
                cloudRepository.getAllWorkflows(NETWORK_PAGE_SIZE, after)
            } else {
                cloudRepository.getAllWorkflows(NETWORK_PAGE_SIZE)
            }

            if(response?.workflows!!.isNotEmpty()){
                stateLiveData.postValue(LoadState.DOWNLOADING)
                response.workflows.let {
                    // Construct workflow devices-list
                    it.forEach { workflow ->
                        // Store accountID
                        workflow.accountID = SharedPrefHelper.getSelectedAccountID()
                        // Parse AUDs into devices
                        workflow.workflowDevices = arrayListOf()
                        workflow.workflowAUDs.forEach { aud ->
                            workflow.workflowDevices!!.add(
                                WorkflowDeviceModel(
                                    aud.substring(3, aud.length),
                                    AppConstants.DEVICE_STATE_PENDING
                                )
                            )
                        }
                        // Fetch SDA_token
                        val sdaTokenResponse = fetchAndSaveSDAToken(workflow)
                        if(sdaTokenResponse != null){
                            workflow.sdaToken = sdaTokenResponse
                        }
                        // Download Task Assets
                        downloadTaskAssets(cloudRepository,
                            workflow.workflowID, workflow.workflowTasks)

                        // FixME
                        // Sync workflow
                        /*if(workflow.workflowID == "016b22375e6e423cce18a69800000000" &&
                                workflow.workflowStatus != WORKFLOW_STATE_SYNCED){
                            val isSyncSuccessful = cloudRepository.syncWorkflow(workflow.workflowID)
                            if(isSyncSuccessful){
                                workflow.workflowStatus = WORKFLOW_STATE_SYNCED
                                LogHelper.debug(TAG, "Workflow synced successfully")
                            } else {
                                LogHelper.debug(TAG, "Workflow sync failed")
                            }
                        }*/
                    }
                    // Store in DB
                    localCache.insertWorkflows(it) {
                        LogHelper.debug(TAG, "LocalCache->Success()")
                        stateLiveData.postValue(LoadState.DOWNLOADED)
                        saveFinished()
                    }
                }
            } else {
                LogHelper.debug(TAG, "Network data-unavailable")
                stateLiveData.postValue(LoadState.FAILED)
                saveFinished()
            }
        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            saveFinished()
        }
    }

    private suspend fun fetchAndSaveSDAToken(workflow: WorkflowModel): SDATokenResponse? {
        // Fetch permission-scope
        val permissionScope = getPermissionScopeFromTasks(workflow.workflowTasks)
        // Fetch audience
        val audienceList = getAudienceListFromDevices(workflow.workflowDevices!!)
        // Call access-token request
        return fetchSDAToken(cloudRepository, permissionScope, audienceList)
    }
}