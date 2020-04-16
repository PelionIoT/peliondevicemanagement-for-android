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

package com.arm.peliondevicemanagement.services

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.store.WorkflowDataSource
import kotlinx.coroutines.CoroutineScope

class LocalRepository(
    private val scope: CoroutineScope,
    private val cloudRepository: CloudRepository,
    private val localCache: LocalCache
) {

    fun fetchWorkflowsFactory(stateLiveData: MutableLiveData<LoadState>): DataSource.Factory<String, Workflow> {
        return object : DataSource.Factory<String, Workflow>() {
            override fun create(): DataSource<String, Workflow> {
                return WorkflowDataSource(scope, cloudRepository, localCache, stateLiveData)
            }
        }
    }
}