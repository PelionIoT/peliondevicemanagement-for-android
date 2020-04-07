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

package com.arm.peliondevicemanagement.utils

import com.arm.mbed.sda.proxysdk.SdkUtil
import com.arm.mbed.sda.proxysdk.http.CreateAccessTokenRequest
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowTaskModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_TASK_NAME_FILE
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.cache.LocalCache
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.peliondevicemanagement.services.store.WorkflowDataSource
import java.util.*
import java.util.concurrent.Executors

object WorkflowUtils {

    private val TAG: String = WorkflowUtils::class.java.simpleName

    // FixME [ Delete on the basis of user from DB also ]
    fun deleteWorkflowsCache() {
        // Delete workflow-asset files
        val userID = SharedPrefHelper.getSelectedUserID()!!
        val accountID = SharedPrefHelper.getSelectedAccountID()!!
        WorkflowFileUtils.deleteWorkflowAssets(userID)

        // Delete entries from workflow-database
        val workflowDao = AppController.getWorkflowDB().workflowsDao()
        val localCache = LocalCache(workflowDao, Executors.newSingleThreadExecutor())
        localCache.deleteAllWorkflows(accountID){
            LogHelper.debug(TAG, "deleteWorkflowsFromDB() Workflows deleted successfully")
        }
    }

    private fun createSDATokenRequest(popPemKey: String, scope: String, audience: List<String>): String {
        val request = CreateAccessTokenRequest()
        request.grantType = "client_credentials"
        request.cnf = popPemKey
        request.scope = scope
        request.audience = audience

        //LogHelper.debug(TAG, "createSDATokenRequest() -> $request")
        return request.toString()
    }

    private fun validateSDAToken(accessToken: String, popPemKey: String) {
        SdkUtil.validateTokenSanity(accessToken, popPemKey)
    }

    suspend fun fetchSDAToken(
        cloudRepository: CloudRepository,
        scope: String,
        audienceList: List<String>): SDATokenResponse? {
        return try {
            val popPemPubKey = SdkUtil.getPopPemPubKey()
            val request = createSDATokenRequest(popPemPubKey, scope, audienceList)
            val tokenResponse = cloudRepository.getSDAToken(request)
            validateSDAToken(tokenResponse?.accessToken!!, popPemPubKey)
            tokenResponse
        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            null
        }
    }

    fun isValidSDAToken(expiresIn: String): Boolean {
        val tokenExpiryDate =
            PlatformUtils.parseJSONTimeString(expiresIn, AppConstants.DEFAULT_DATE_FORMAT)
        val tokenExpiryTime =
            PlatformUtils.parseJSONTimeString(expiresIn, AppConstants.DEFAULT_TIME_FORMAT)
        val currentDateTime = Date()
        val currentDate =
            PlatformUtils.parseDateTimeString(currentDateTime, AppConstants.DEFAULT_DATE_FORMAT)
        val currentTime =
            PlatformUtils.parseDateTimeString(currentDateTime, AppConstants.DEFAULT_TIME_FORMAT)

        val tokenExpiryDateTime = "$tokenExpiryDate, $tokenExpiryTime"
        val nowDateTime = "$currentDate, $currentTime"

        var tokenStatus = false
        if(tokenExpiryDateTime >= nowDateTime){
            tokenStatus = true
        }
        LogHelper.debug(TAG, "isValidSDAToken() $tokenStatus, " +
                "CurrentDateTime: $nowDateTime " +
                "TokenExpiresOn: $tokenExpiryDateTime")

        return tokenStatus
    }

    fun getPermissionScopeFromTasks(tasks: List<WorkflowTaskModel>): String {
        val scope: String
        val scopeBuffer = StringBuffer()

        tasks.forEach { task ->
            if(task.taskName == AppConstants.READ_TASK){
                scopeBuffer.append(AppConstants.COMMAND_READ)
            }
            if(task.taskName == AppConstants.WRITE_TASK){
                if(scopeBuffer.isNotEmpty()){
                    scopeBuffer.append(" ${AppConstants.COMMAND_CONFIGURE}")
                } else {
                    scopeBuffer.append(AppConstants.COMMAND_CONFIGURE)
                }
            }
        }

        scope = scopeBuffer.toString()
        return scope
    }

    suspend fun downloadTaskAssets(cloudRepository: CloudRepository, workflowID: String, workflowTasks: List<WorkflowTaskModel>) {
        val userID = SharedPrefHelper.getSelectedUserID()
        val accountID = SharedPrefHelper.getSelectedAccountID()
        val filePath = "$userID/$accountID/$workflowID"

        LogHelper.debug(TAG, "Scanning workflow task-assets")
        val fileQueue = arrayListOf<String>()
        workflowTasks.forEach { task ->
            if(task.taskName == AppConstants.WRITE_TASK){
                task.inputParameters.forEach { inputParam ->
                    if(inputParam.paramType == AppConstants.WORKFLOW_TASK_TYPE_FILE &&
                        inputParam.paramName == WORKFLOW_TASK_NAME_FILE){
                        fileQueue.add(inputParam.paramValue)
                    }
                }
            }
        }

        if(!fileQueue.isNullOrEmpty()){
            fileQueue.forEach { fileID ->
                LogHelper.debug(TAG, "Downloading file: $fileID")
                val fileResponse = cloudRepository.getWorkflowTaskAssetFile(fileID)
                if(fileResponse != null){
                    val inputStream = fileResponse.byteStream()
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

}