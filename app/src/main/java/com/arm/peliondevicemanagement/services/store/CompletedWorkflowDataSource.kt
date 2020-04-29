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

import androidx.paging.PageKeyedDataSource
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.constants.AppConstants.DATABASE_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.state.workflow.WorkflowState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.cache.LocalCache

class CompletedWorkflowDataSource(
    private val localCache: LocalCache
): PageKeyedDataSource<String, Workflow>()  {

    companion object {
        private val TAG: String = CompletedWorkflowDataSource::class.java.simpleName
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, Workflow>
    ) {
        val workflowList: List<Workflow> = localCache
            .fetchWorkflowsByStatus(DATABASE_PAGE_SIZE,
                WorkflowState.COMPLETED.name)

        if(workflowList.isEmpty()){
            LogHelper.debug(TAG, "loadInitial() No more items available")
            callback.onResult(listOf(), null, null)
        } else {
            val lastItem = workflowList.last()
            LogHelper.debug(TAG, "loadInitial() loadedItems: ${workflowList.size}, " +
                    "afterID: ${lastItem.workflowID}")
            callback.onResult(workflowList, null, lastItem.workflowID)
        }
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, Workflow>
    ) {
        LogHelper.debug(TAG, "loadAfter() afterID: ${params.key}")
        val workflowList: List<Workflow> = localCache
            .fetchWorkflowsByStatus(DATABASE_PAGE_SIZE,
                WorkflowState.COMPLETED.name, params.key)

        if(workflowList.isEmpty()){
            LogHelper.debug(TAG, "loadAfter() No more items available")
            callback.onResult(listOf(), params.key)
        } else {
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
}