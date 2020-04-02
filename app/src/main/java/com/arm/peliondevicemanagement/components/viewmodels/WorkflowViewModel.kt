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
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.constants.AppConstants.DATABASE_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.LocalRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.cache.WorkflowDB
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

    private var workflowLiveData: LiveData<PagedList<WorkflowModel>>
    private val refreshStateLiveData = MutableLiveData<LoadState>()

    init {
        localCache = LocalCache(workflowDB.workflowsDao(), Executors.newSingleThreadExecutor())
        localRepository = LocalRepository(scope, cloudRepository, localCache)
        workflowLiveData = getWorkflowsLiveData()
    }

    fun getWorkflows(): LiveData<PagedList<WorkflowModel>> = workflowLiveData

    fun getRefreshState(): LiveData<LoadState> = refreshStateLiveData

    fun refresh() {
        workflowLiveData.value?.dataSource?.invalidate()
    }

    private fun getWorkflowsLiveData(): LiveData<PagedList<WorkflowModel>> {
        val pageConfig = PagedList.Config.Builder()
            .setPageSize(DATABASE_PAGE_SIZE)
            .setInitialLoadSizeHint(DATABASE_PAGE_SIZE)
            .setEnablePlaceholders(false)
            .build()

        // Fetch data from the local-cache and return a factory
        val dataSourceFactory = localRepository.fetchWorkflowsFactory()

        // Get the paged list
        return LivePagedListBuilder(dataSourceFactory, pageConfig)
            .setBoundaryCallback(object: PagedList.BoundaryCallback<WorkflowModel>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    // Handle empty initial load here
                    LogHelper.debug(TAG, "BoundaryCallback()->onZeroItemsLoaded")
                    refreshStateLiveData.postValue(LoadState.EMPTY)
                }

                override fun onItemAtEndLoaded(itemAtEnd: WorkflowModel) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    // Here you can listen to last item on list
                    LogHelper.debug(TAG, "BoundaryCallback()->onItemAtEndLoaded")
                    refreshStateLiveData.postValue(LoadState.LOADED)
                }

                override fun onItemAtFrontLoaded(itemAtFront: WorkflowModel) {
                    super.onItemAtFrontLoaded(itemAtFront)
                    // Here you can listen to first item on list
                    LogHelper.debug(TAG, "BoundaryCallback()->onItemAtFrontLoaded")
                    refreshStateLiveData.postValue(LoadState.LOADED)
                }
            })
            .build()
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}