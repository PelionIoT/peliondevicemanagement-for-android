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

import androidx.room.*
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import retrofit2.http.DELETE

@Dao
interface WorkflowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkflows(workflows: List<WorkflowModel>)

    @Query("SELECT * FROM workflows ORDER BY workflowCreatedAt ASC LIMIT 10")
    fun fetchWorkflows(): List<WorkflowModel>

    @Query("SELECT * FROM workflows WHERE workflowCreatedAt > " +
            "(SELECT workflowCreatedAt FROM workflows WHERE workflowID LIKE :afterID) " +
            "ORDER BY workflowCreatedAt ASC LIMIT 10")
    fun fetchWorkflows(afterID: String): List<WorkflowModel>

    @Query("SELECT * FROM workflows WHERE workflowID=:workflowID")
    fun fetchWorkflow(workflowID: String): WorkflowModel

    @Update
    fun updateWorkflow(workflow: WorkflowModel)

    @Query("UPDATE workflows SET workflowStatus=:status WHERE workflowID=:workflowID")
    fun updateWorkflowStatus(workflowID: String, status: String)

    // FixME
    /*@Query("UPDATE workflows SET workflowDevices=:devices WHERE workflowID=:workflowID")
    fun updateWorkflowDevices(workflowID: String, devices: List<WorkflowDeviceModel>)*/

    /*@DELETE
    fun deleteWorkflow(workflowID: String)*/

    @Query("DELETE FROM workflows")
    fun deleteAllWorkflows()

}