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

package com.arm.peliondevicemanagement.utils

import com.arm.mbed.sda.proxysdk.SdkUtil
import com.arm.mbed.sda.proxysdk.http.CreateAccessTokenRequest
import com.arm.mbed.sda.proxysdk.operation.OperationArgumentType
import com.arm.mbed.sda.proxysdk.operation.ParamElement
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceRunLogs
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.task.*
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ERROR_CODE
import com.arm.peliondevicemanagement.constants.AppConstants.COMMAND_CONFIGURE
import com.arm.peliondevicemanagement.constants.AppConstants.COMMAND_READ
import com.arm.peliondevicemanagement.constants.AppConstants.READ_TASK
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_GRANT_TYPE
import com.arm.peliondevicemanagement.constants.AppConstants.TASK_NAME_FILE
import com.arm.peliondevicemanagement.constants.AppConstants.TASK_NAME_FILEPATH
import com.arm.peliondevicemanagement.constants.AppConstants.TASK_TYPE_FILE
import com.arm.peliondevicemanagement.constants.AppConstants.TASK_TYPE_STRING
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_OUT_ASSETS_FILENAME
import com.arm.peliondevicemanagement.constants.AppConstants.WRITE_TASK
import com.arm.peliondevicemanagement.constants.ExecutionMode
import com.arm.peliondevicemanagement.constants.state.workflow.device.DeviceRunState
import com.arm.peliondevicemanagement.constants.state.workflow.task.TaskRunState
import com.arm.peliondevicemanagement.constants.state.workflow.task.TaskTypeState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.transport.sda.CommandConstants
import com.arm.peliondevicemanagement.transport.sda.DeviceCommand
import com.arm.peliondevicemanagement.utils.WorkflowFileUtils.isFileExists
import com.arm.peliondevicemanagement.utils.WorkflowFileUtils.readWorkflowAssetFile
import com.arm.peliondevicemanagement.utils.WorkflowFileUtils.writeWorkflowAssetFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object WorkflowUtils {

    private val TAG: String = WorkflowUtils::class.java.simpleName

    // FixME [ Delete on the basis of user from DB also ]
    fun deleteWorkflowsCache() {
        // Delete workflow-asset files
        val userID = SharedPrefHelper.getSelectedUserID()!!
        val accountID = SharedPrefHelper.getSelectedAccountID()
        WorkflowFileUtils.deleteWorkflowAssets(userID)

        // Delete entries from workflow-database
        val workflowDao = AppController.getWorkflowDB().workflowsDao()
        val localCache = LocalCache(workflowDao, Executors.newSingleThreadExecutor())
        localCache.deleteAllWorkflows(accountID){
            LogHelper.debug(TAG, "deleteWorkflowsFromDB() Workflows deleted successfully")
        }
    }

    fun deleteWorkflowsCacheExceptStatus(workflowStatus: String, deleted: () -> Unit) {
        // Delete workflow-asset files
        val userID = SharedPrefHelper.getSelectedUserID()!!
        val accountID = SharedPrefHelper.getSelectedAccountID()
        WorkflowFileUtils.deleteWorkflowAssets(userID)

        // Delete entries from workflow-database
        val workflowDao = AppController.getWorkflowDB().workflowsDao()
        val localCache = LocalCache(workflowDao, Executors.newSingleThreadExecutor())
        localCache.deleteAllWorkflowsExceptStatus(accountID, workflowStatus){
            LogHelper.debug(TAG, "deleteWorkflowsFromDB() Workflows deleted successfully")
            deleted()
        }
    }

    fun createSDATokenRequest(popPemKey: String, scope: String, audience: List<String>): String {
        val request = CreateAccessTokenRequest()
        request.grantType = SDA_GRANT_TYPE
        request.cnf = popPemKey
        request.scope = scope
        request.audience = audience

        //LogHelper.debug(TAG, "createSDATokenRequest() -> $request")
        return request.toString()
    }

    fun getSDAPopPemPubKey(): String {
        if(SharedPrefHelper.getSDAPopPemPubKey().isEmpty()){
            val popPemPubKey = SdkUtil.getPopPemPubKey()
            SharedPrefHelper.storeSDAPopPemPubKey(popPemPubKey)
        }
        return SharedPrefHelper.getSDAPopPemPubKey().trim()
    }

    fun validateSDATokenSanity(accessToken: String, popPemKey: String) {
        //LogHelper.debug(TAG, "DoSanityCheck-> accessToken: $accessToken, popPemKey: $popPemKey")
        SdkUtil.validateTokenSanity(accessToken, popPemKey)
    }

    suspend fun fetchSDAToken(
        cloudRepository: CloudRepository,
        scope: String,
        audienceList: List<String>): SDATokenResponse? {
        return try {
            val popPemPubKey = getSDAPopPemPubKey()
            val request = createSDATokenRequest(popPemPubKey, scope, audienceList)
            //LogHelper.debug(TAG, "SDA_Token_Request-> $request")
            val tokenResponse = cloudRepository.getSDAToken(request)
            validateSDATokenSanity(tokenResponse?.accessToken!!, popPemPubKey)
            //LogHelper.debug(TAG, "TokenSanity->Passed")
            tokenResponse
        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            null
        }
    }

    fun isValidSDAToken(expiresIn: String): Boolean {
        val tokenExpiryDate = PlatformUtils
            .convertJSONDateTimeStringToDate(expiresIn)
        val currentDate = Date()

        var tokenStatus = false
        if(tokenExpiryDate.after(currentDate) && tokenExpiryDate.time > System.currentTimeMillis()){
            tokenStatus = true
        }

        LogHelper.debug(TAG, "isValidSDAToken() $tokenStatus, " +
                "CurrentDateTime: $currentDate " +
                "TokenExpiresOn: $tokenExpiryDate")

        return tokenStatus
    }

    suspend fun isWorkflowAssetsDownloaded(workflowID: String, tasks: List<WorkflowTask>): Boolean {
        return suspendCoroutine {
            val userID = SharedPrefHelper.getSelectedUserID()
            val accountID = SharedPrefHelper.getSelectedAccountID()
            var filePath = "$userID/$accountID/$workflowID"

            LogHelper.debug(TAG, "Looking for downloaded assets")
            var isAssetDownloaded = false
            tasks.forEach { task ->
                if(task.taskName == WRITE_TASK){
                    task.inputParameters.forEach { inputParam ->
                        if(inputParam.paramType == TASK_TYPE_FILE &&
                            inputParam.paramName == TASK_NAME_FILE){
                            filePath += "/${task.taskID}"
                            isAssetDownloaded = isFileExists(filePath, inputParam.paramValue)
                            LogHelper.debug(TAG, "Found write-file task, asset-available: $isAssetDownloaded")
                        }
                    }
                }
            }
            LogHelper.debug(TAG, "Workflow assets available: $isAssetDownloaded")
            it.resume(isAssetDownloaded)
        }
    }

    fun isWriteTaskAvailable(tasks: List<WorkflowTask>): Boolean {
        var status = false
        tasks.forEach { task->
            if(task.taskName == WRITE_TASK){
                status = true
                return status
            }
        }
        return status
    }

    fun isReadTaskType(taskID: String, tasks: List<WorkflowTask>): Boolean {
        var status = false
        tasks.forEach { task->
            if(taskID == task.taskID && task.taskName == READ_TASK){
                status = true
                return status
            }
        }
        return status
    }

    fun getPermissionScopeFromTasks(tasks: List<WorkflowTask>): String {
        val scope: String
        val scopeBuffer = StringBuffer()

        tasks.forEach { task ->
            if(task.taskName == READ_TASK){
                if(scopeBuffer.isNotEmpty()){
                    scopeBuffer.append(" $COMMAND_READ")
                } else {
                    scopeBuffer.append(COMMAND_READ)
                }
            }
            if(task.taskName == WRITE_TASK){
                if(scopeBuffer.isNotEmpty()){
                    scopeBuffer.append(" $COMMAND_CONFIGURE")
                } else {
                    scopeBuffer.append(COMMAND_CONFIGURE)
                }
            }
        }

        scope = scopeBuffer.toString()
        return scope
    }

    fun getWorkflowTaskIDs(tasks: List<WorkflowTask>): List<String> {
        val listOfTaskIDs = arrayListOf<String>()
        tasks.forEach { task ->
            if(task.taskName == READ_TASK){
                listOfTaskIDs.add(task.taskID)
            }
        }
        return listOfTaskIDs
    }

    fun getAudienceListFromDevices(devices: List<WorkflowDevice>): List<String> {
        val audienceList = arrayListOf<String>()
        devices.forEach { device ->
            val audience = "ep:${device.deviceName}"
            audienceList.add(audience)
        }
        return audienceList
    }

    suspend fun downloadTaskAssets(cloudRepository: CloudRepository, workflowID: String, workflowTasks: List<WorkflowTask>) {
        val userID = SharedPrefHelper.getSelectedUserID()
        val accountID = SharedPrefHelper.getSelectedAccountID()
        var filePath = "$userID/$accountID/$workflowID"

        LogHelper.debug(TAG, "Scanning workflow task-assets")
        val fileQueue = hashMapOf<String, String>()
        workflowTasks.forEach { task ->
            if(task.taskName == WRITE_TASK){
                task.inputParameters.forEach { inputParam ->
                    if(inputParam.paramType == TASK_TYPE_FILE &&
                        inputParam.paramName == TASK_NAME_FILE){
                        fileQueue[task.taskID] = inputParam.paramValue
                    }
                }
            }
        }

        if(!fileQueue.isNullOrEmpty()){
            fileQueue.forEach { fileMap ->
                val fileID = fileMap.value
                LogHelper.debug(TAG, "Downloading file: $fileID")
                val fileResponse = cloudRepository.getWorkflowTaskAssetFile(fileID)
                if(fileResponse != null){
                    val inputStream = fileResponse.byteStream()
                    // Append taskID to filePath
                    filePath += "/${fileMap.key}"
                    LogHelper.debug(TAG, "Saving to $filePath")
                    val isSuccessful = WorkflowFileUtils.downloadWorkflowAssetFile(
                        filePath,
                        fileID,
                        inputStream,
                        fileResponse.contentLength()
                    )
                    if(isSuccessful){
                        LogHelper.debug(TAG, "Download successful: $fileID")
                    } else {
                        LogHelper.debug(TAG,"Download failed: $fileID")
                    }
                } else {
                    LogHelper.debug(TAG,"Download failed: $fileID")
                }
            }
        } else {
            LogHelper.debug(TAG, "No downloadable assets found.")
        }

    }

    fun fetchTaskOutputAsset(workflowID: String, taskID: String): File? {
        val userID = SharedPrefHelper.getSelectedUserID()
        val accountID = SharedPrefHelper.getSelectedAccountID()
        val filePath = "$userID/$accountID/$workflowID/$taskID"

        LogHelper.debug(TAG, "Looking for asset with taskID: $taskID")
        val fileForUpload = readWorkflowAssetFile(filePath)

        if(fileForUpload != null){
            LogHelper.debug(TAG, "Found asset with taskID: $taskID")
        }

        return fileForUpload
    }

    fun getDeviceCommands(workflowID: String, tasks: List<WorkflowTask>): List<TaskDeviceCommand> {
        val userID = SharedPrefHelper.getSelectedUserID()
        val accountID = SharedPrefHelper.getSelectedAccountID()


        val deviceCommandList = arrayListOf<TaskDeviceCommand>()
        LogHelper.debug(TAG, "Scanning tasks to construct device-commands")
        tasks.forEach {task ->
            //LogHelper.debug(TAG, "Found task: ${task.taskName}")
            when(task.taskName){
                READ_TASK -> {
                    task.inputParameters.forEach { param ->
                        if(param.paramName == TASK_NAME_FILEPATH &&
                            param.paramType == TASK_TYPE_STRING
                        ) {
                            val fileName = param.paramValue
                            val commandParams = arrayOf(ParamElement(OperationArgumentType.STR, fileName))
                            val deviceCommand = DeviceCommand(CommandConstants.READ, commandParams)
                            LogHelper.debug(TAG, "Adding device-command: $deviceCommand")
                            deviceCommandList.add(TaskDeviceCommand(task.taskID, deviceCommand))
                        }
                    }
                }
                WRITE_TASK -> {
                    var fileName: String? = null
                    var fileContent: String? = null
                    task.inputParameters.forEach { param ->
                        if(param.paramName == TASK_NAME_FILEPATH &&
                            param.paramType == TASK_TYPE_STRING) {
                            fileName = param.paramValue
                        }

                        if(param.paramName == TASK_NAME_FILE &&
                            param.paramType == TASK_TYPE_FILE) {
                            // Construct filePath
                            val filePath = "$userID/$accountID/$workflowID/${task.taskID}"
                            // Read the file
                            val content = readWorkflowAssetFile(filePath, param.paramValue)
                            if(content != null){
                                fileContent = content
                                //LogHelper.debug(TAG, "FileRead: $fileContent")
                            }
                        }
                    }

                    if(!fileName.isNullOrEmpty() && !fileContent.isNullOrEmpty()){

                        val fileNameAndContent = "$fileName^$fileContent"

                        // Construct command
                        val commandParams = arrayOf(
                            ParamElement(OperationArgumentType.STR, fileNameAndContent)
                        )

                        val deviceCommand = DeviceCommand(CommandConstants.CONFIGURE, commandParams)
                        LogHelper.debug(TAG, "Adding device-command: $deviceCommand")
                        deviceCommandList.add(TaskDeviceCommand(task.taskID, deviceCommand))
                    }
                }
            }
        }
        return deviceCommandList
    }

    fun createTaskRunLog(taskID: String, state: TaskRunState, fileID: String? = null): TaskRun {
        val outputParamsList = ArrayList<TaskRunParam>()
        when(state) {
            TaskRunState.SUCCEEDED -> {
                outputParamsList.add(
                    TaskRunParam(KEY_ERROR_CODE, TaskTypeState.NUMBER.name, "0")
                )
                if(fileID != null){
                    outputParamsList.add(
                        TaskRunParam(TASK_NAME_FILE, TaskTypeState.FILE.name, fileID)
                    )
                }
                return TaskRun(taskID, TaskRunState.SUCCEEDED.name, outputParamsList)
            }
            TaskRunState.FAILED -> {
                outputParamsList.add(
                    TaskRunParam(KEY_ERROR_CODE, TaskTypeState.NUMBER.name, "-1")
                )
                return TaskRun(taskID, TaskRunState.FAILED.name, outputParamsList)
            }
            TaskRunState.SKIPPED -> {
                return TaskRun(taskID, TaskRunState.SKIPPED.name, null)
            }
        }
    }

    fun createDeviceRunLog(workflowID: String, deviceID: String,
                           location: String, executionTime: String,
                           log: String, taskRuns: ArrayList<TaskRun>,
                           failedCount: Int = 0): DeviceRunLogs {
        return if(failedCount > 0){
            DeviceRunLogs(
                workflowID, deviceID,
                DeviceRunState.HAS_FAILURES.name, location,
                log, executionTime, taskRuns
            )
        } else {
            DeviceRunLogs(
                workflowID, deviceID,
                DeviceRunState.SUCCEEDED.name, location,
                log, executionTime, taskRuns
            )
        }
    }

    fun getSDAExecutionMode(): ExecutionMode {
        return when {
            SharedPrefHelper.getDeveloperOptions().getSDAExecutionMode().isEmpty() -> {
                SharedPrefHelper.getDeveloperOptions()
                    .storeSDAExecutionMode(ExecutionMode.PHYSICAL.name)
                ExecutionMode.PHYSICAL
            }
            SharedPrefHelper.getDeveloperOptions().getSDAExecutionMode() == ExecutionMode.PHYSICAL.name -> {
                ExecutionMode.PHYSICAL
            }
            else -> {
                ExecutionMode.VIRTUAL
            }
        }
    }

    fun saveWorkflowTaskOutputAsset(workflowID: String,
                                    taskID: String,
                                    fileContent: ByteArray,
                                    saveFinished: (status: Boolean) -> Unit){
        val userID = SharedPrefHelper.getSelectedUserID()
        val accountID = SharedPrefHelper.getSelectedAccountID()
        val filePath = "$userID/$accountID/$workflowID/$taskID"

        LogHelper.debug(TAG, "Saving device-output to $filePath")

        val ioExecutor = Executors.newSingleThreadExecutor()
        ioExecutor.execute {
            val isSuccessful = writeWorkflowAssetFile(filePath, WORKFLOW_OUT_ASSETS_FILENAME, fileContent)
            saveFinished(isSuccessful)
        }
    }

    fun convertDeviceRunLogsToJson(deviceRunLogs: DeviceRunLogs): String {
        val type = object : TypeToken<DeviceRunLogs>() {}.type
        return Gson().toJson(deviceRunLogs, type)
    }

    fun verifyDeviceRunLogsStatus(deviceRunLogs: DeviceRunLogs): DeviceRunLogs {
        LogHelper.debug(TAG, "Verify device-run logs: $deviceRunLogs")
        if(deviceRunLogs.deviceStatus == DeviceRunState.SUCCEEDED.name){
            // Check the task-runs for failures
            deviceRunLogs.deviceTaskRuns.forEach { taskRun ->
                if(taskRun.taskStatus != TaskRunState.SUCCEEDED.name){
                    LogHelper.debug(TAG, "Found a malformed run-log, updating device-run status")
                    deviceRunLogs.deviceStatus = DeviceRunState.HAS_FAILURES.name
                }
            }
        }
        return deviceRunLogs
    }

    /* FixME [ To be removed later ]
    fun markSuccessFailure(deviceRunLogs: DeviceRunLogs): DeviceRunLogs {
        if(deviceRunLogs.deviceStatus == DeviceRunState.HAS_FAILURES.name){
            LogHelper.debug(TAG, "Malforming the run-logs")
            deviceRunLogs.deviceStatus = DeviceRunState.SUCCEEDED.name
        }
        LogHelper.debug(TAG, "Malformed logs: $deviceRunLogs")
        return deviceRunLogs
    }*/

}