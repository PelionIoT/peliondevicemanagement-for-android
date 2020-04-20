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

package com.arm.peliondevicemanagement.services.cache

import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import java.util.concurrent.Executor

class LocalCache(
    private val workflowDao: WorkflowDao,
    private val ioExecutor: Executor
) {

    companion object {
        private val TAG: String = LocalCache::class.java.simpleName
    }

    fun insertWorkflows(workflows: List<Workflow>,
                        insertFinished: () -> Unit) {
        ioExecutor.execute {
            LogHelper.debug(TAG, "Storing ${workflows.size} workflows")
            workflowDao.insertWorkflows(workflows)
            insertFinished()
        }
    }

    fun updateSDAToken(workflowID: String,
                       sdaToken: SDATokenResponse?,
                       updateFinished: () -> Unit) {
        ioExecutor.execute {
            LogHelper.debug(TAG, "Updating SDA-Token of workflow: $workflowID")
            workflowDao.updateWorkflowSDAToken(workflowID, sdaToken)
            updateFinished()
        }
    }

    fun updateWorkflowDevices(workflowID: String,
                              devices: ArrayList<WorkflowDevice>,
                              updateFinished: () -> Unit) {
        ioExecutor.execute {
            LogHelper.debug(TAG, "Updating ${devices.size} device of workflow: $workflowID")
            workflowDao.updateWorkflowDevices(workflowID, devices)
            updateFinished()
        }
    }

    fun fetchWorkflows(limit: Int,
                       after: String? = null): List<Workflow> {
        LogHelper.debug(TAG, "fetchWorkflows()")
        //val assigneeID = SharedPrefHelper.getSelectedUserID()!!
        val accountID = SharedPrefHelper.getSelectedAccountID()!!
        return if(after != null){
            workflowDao.fetchWorkflows(accountID, limit, after)
        } else {
            workflowDao.fetchWorkflows(accountID, limit)
        }
    }

    fun fetchWorkflow(workflowID: String): Workflow {
        val accountID = SharedPrefHelper.getSelectedAccountID()!!
        return workflowDao.fetchWorkflow(accountID, workflowID)
    }

    fun deleteAllWorkflows(accountID: String,
                           deleteComplete: () -> Unit) {
        ioExecutor.execute {
            LogHelper.debug(TAG, "Deleting workflows of account: $accountID")
            workflowDao.deleteAllWorkflows(accountID)
            deleteComplete()
        }
    }
}