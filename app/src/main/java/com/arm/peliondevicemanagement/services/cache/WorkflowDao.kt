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
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.helpers.converters.SDATokenResponseConverter
import com.arm.peliondevicemanagement.helpers.converters.WDevicesListConverter
import com.arm.peliondevicemanagement.services.data.SDATokenResponse

@Dao
interface WorkflowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkflows(workflows: List<Workflow>)

    // FixME [ Add assignee field after testing ]
    @Query("SELECT * FROM workflows WHERE accountID=:accountID ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflows(accountID: String, limit: Int): List<Workflow>

    @Query("SELECT * FROM workflows WHERE accountID=:accountID AND (pKey > " +
            "(SELECT pKey FROM workflows WHERE workflowID LIKE :afterID)) " +
            "ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflows(accountID: String, limit: Int, afterID: String): List<Workflow>

    @Query("SELECT * FROM workflows WHERE (accountID=:accountID AND workflowStatus=:workflowStatus) ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflowByStatus(accountID: String, limit: Int, workflowStatus: String): List<Workflow>

    @Query("SELECT * FROM workflows WHERE (accountID=:accountID AND workflowStatus=:workflowStatus AND (pKey > " +
            "(SELECT pKey FROM workflows WHERE workflowID LIKE :afterID))) " +
            "ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflowByStatus(accountID: String, limit: Int, workflowStatus: String, afterID: String): List<Workflow>

    @Query("SELECT * FROM workflows WHERE (accountID=:accountID AND (workflowStatus=:statusOne OR workflowStatus=:statusTwo)) ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflowByMultiStatus(accountID: String, limit: Int, statusOne: String, statusTwo: String): List<Workflow>

    @Query("SELECT * FROM workflows WHERE (accountID=:accountID AND (workflowStatus=:statusOne OR workflowStatus=:statusTwo)" +
            " AND (pKey > (SELECT pKey FROM workflows WHERE workflowID LIKE :afterID))) " +
            "ORDER BY pKey ASC LIMIT :limit")
    fun fetchWorkflowByMultiStatus(accountID: String, limit: Int, statusOne: String, statusTwo: String, afterID: String): List<Workflow>

    @Query("SELECT * FROM workflows WHERE (accountID=:accountID AND workflowID=:workflowID)")
    fun fetchSingleWorkflow(accountID: String, workflowID: String): Workflow?

    @Update
    fun updateWorkflow(workflow: Workflow)

    @Query("UPDATE workflows SET workflowStatus=:workflowStatus WHERE (accountID=:accountID AND workflowID=:workflowID)")
    fun updateWorkflowStatus(accountID: String, workflowID: String, workflowStatus: String)


    @TypeConverters(SDATokenResponseConverter::class)
    @Query("UPDATE workflows SET sdaToken=:sdaToken WHERE workflowID=:workflowID")
    fun updateWorkflowSDAToken(workflowID: String, sdaToken: SDATokenResponse?)

    /*@TypeConverters(WDevicesListConverter::class)
    @Query("SELECT workflowDevices FROM workflows WHERE workflowID=:workflowID")
    fun fetchWorkflowDevicesByWorkflowID(workflowID: String): List<WorkflowDevice>?*/

    @TypeConverters(WDevicesListConverter::class)
    @Query("UPDATE workflows SET workflowDevices=:devices WHERE workflowID=:workflowID")
    fun updateWorkflowDevices(workflowID: String, devices: ArrayList<WorkflowDevice>)

    /*@DELETE
    fun deleteWorkflow(workflowID: String)*/

    @Query("DELETE FROM workflows WHERE accountID=:accountID")
    fun deleteAllWorkflows(accountID: String)

}