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

package com.arm.peliondevicemanagement.services.repository

import com.arm.peliondevicemanagement.components.models.user.AccountProfile
import com.arm.peliondevicemanagement.components.models.LicenseModel
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.constants.APIConstants.CONTENT_TYPE_JSON
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ACCOUNT
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ACCOUNT_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_GRANT_TYPE
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_PASSWORD
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_USERNAME
import com.arm.peliondevicemanagement.constants.BrandingTheme
import com.arm.peliondevicemanagement.services.api.CloudAPIService
import com.arm.peliondevicemanagement.services.api.CloudAPIService.Companion.createJSONRequestBody
import com.arm.peliondevicemanagement.services.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.util.*

class CloudRepository(private val cloudAPIService: CloudAPIService): BaseRepository() {

    suspend fun doAuth(username: String, password: String, accountID: String = ""): LoginResponse? {
        val loginResponse: LoginResponse?

        if(accountID.isNotEmpty()){
            loginResponse = doSafeAPIRequest(
                call = { cloudAPIService.doAuth(createJSONRequestBody(
                    KEY_USERNAME to username,
                    KEY_PASSWORD to password,
                    KEY_GRANT_TYPE to KEY_PASSWORD,
                    KEY_ACCOUNT to accountID))
                }
            )
        } else {
            loginResponse = doSafeAPIRequest(
                call = { cloudAPIService.doAuth(createJSONRequestBody(
                    KEY_USERNAME to username,
                    KEY_PASSWORD to password,
                    KEY_GRANT_TYPE to KEY_PASSWORD))
                }
            )
        }
        return loginResponse
    }

    suspend fun doImpersonate(accountID: String): LoginResponse? {
        return doSafeAPIRequest(
            call = { cloudAPIService.doImpersonate(createJSONRequestBody(
                    KEY_ACCOUNT_ID to accountID
                ))
            }
        )
    }

    suspend fun getSDAToken(request: String): SDATokenResponse? {
        return doSafeAPIRequest(
            call = { cloudAPIService.getSDAToken(
                request.toRequestBody(CONTENT_TYPE_JSON))
            }
        )
    }

    suspend fun getUserProfile(): UserProfile? {
        return doSafeAPIRequest(
        call = { cloudAPIService.getUserProfile()}
        )
    }

    suspend fun getAccountProfile(): AccountProfile? {
        return doSafeAPIRequest(
            call = { cloudAPIService.getAccountProfile()}
        )
    }

    suspend fun getAssignedWorkflows(itemsPerPage: Int,
                                     assigneeID: String,
                                     after: String? = null): WorkflowsResponse? {
        return if(after != null){
            doSafeAPIRequest(
                call = { cloudAPIService
                    .getAssignedWorkflows(itemsPerPage, assigneeID, after)}
            )
        } else {
            doSafeAPIRequest(
                call = { cloudAPIService
                    .getAssignedWorkflows(itemsPerPage, assigneeID)}
            )
        }
    }

    suspend fun syncWorkflow(workflowID: String): Boolean {
        val status: Boolean
        val response = doSafeAPIRequest(
            call = { cloudAPIService.syncWorkflow(workflowID)}
        )
        status = (response?.contentLength()!!.toInt() == 0)
        return status
    }

    suspend fun getWorkflowTaskAssetFile(fileID: String): ResponseBody? {
        return doSafeAPIRequest(
            call = { cloudAPIService.getWorkflowTaskAssetFile(fileID)}
        )
    }

    suspend fun uploadWorkflowTaskAssetFile(filePart: MultipartBody.Part): FileUploadResponse? {
        return doSafeAPIRequest(
            call = { cloudAPIService.uploadWorkflowTaskAssetFile(filePart)}
        )
    }

    suspend fun uploadDeviceRunLogs(json: String): DeviceRunUploadResponse? {
        return doSafeAPIRequest(
            call = { cloudAPIService.uploadRunLogs(json.toRequestBody(CONTENT_TYPE_JSON))}
        )
    }

    suspend fun getBrandingImages(accountID: String, theme: BrandingTheme): BrandingImageResponse? {
        return doSafeAPIRequest(
            call = { cloudAPIService.getAccountBrandingImages(accountID,
                theme.name.toLowerCase(Locale.ENGLISH)) }
        )
    }
}