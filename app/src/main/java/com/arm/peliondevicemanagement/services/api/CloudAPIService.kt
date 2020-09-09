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

package com.arm.peliondevicemanagement.services.api

import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.components.models.user.AccountProfile
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.constants.APIConstants.API_ACCOUNTS
import com.arm.peliondevicemanagement.constants.APIConstants.API_ACCOUNTS_ME
import com.arm.peliondevicemanagement.constants.APIConstants.API_ALL_WORKFLOWS
import com.arm.peliondevicemanagement.constants.APIConstants.API_LOGIN
import com.arm.peliondevicemanagement.constants.APIConstants.API_IMPERSONATE
import com.arm.peliondevicemanagement.constants.APIConstants.API_USER_ME
import com.arm.peliondevicemanagement.constants.APIConstants.API_ASSIGNED_WORKFLOWS
import com.arm.peliondevicemanagement.constants.APIConstants.API_BRANDING_COLORS
import com.arm.peliondevicemanagement.constants.APIConstants.API_BRANDING_IMAGES
import com.arm.peliondevicemanagement.constants.APIConstants.API_CAPTCHA
import com.arm.peliondevicemanagement.constants.APIConstants.API_DEVICES
import com.arm.peliondevicemanagement.constants.APIConstants.API_DEVICE_ENROLLMENTS
import com.arm.peliondevicemanagement.constants.APIConstants.API_SDA_TOKEN
import com.arm.peliondevicemanagement.constants.APIConstants.API_WORKFLOW_DEVICE_RUNS
import com.arm.peliondevicemanagement.constants.APIConstants.API_WORKFLOW_FILES
import com.arm.peliondevicemanagement.constants.APIConstants.API_WORKFLOW_SYNC
import com.arm.peliondevicemanagement.constants.APIConstants.CONTENT_TYPE_JSON
import com.arm.peliondevicemanagement.constants.APIConstants.DEFAULT_BASE_URL
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ACCOUNT_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_AFTER_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ASSIGNEE_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_AUTHORIZATION
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_BEARER
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_CONTENT_TYPE
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_CONTENT_TYPE_JSON
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_FILE_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_FILTER
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_LIMIT
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ORDER
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_THEME
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_WORKFLOW_ID
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.data.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface CloudAPIService {

    companion object {
        private val TAG = CloudAPIService::class.java.simpleName

        operator fun invoke(): CloudAPIService {

            val requestInterceptor = Interceptor { chain->

                val newRequest = chain.request()
                    .newBuilder()
                    .addHeader(KEY_CONTENT_TYPE, KEY_CONTENT_TYPE_JSON)

                if(!SharedPrefHelper.getUserAccessToken().isNullOrBlank()){
                    newRequest.addHeader(KEY_AUTHORIZATION,
                        KEY_BEARER + " ${SharedPrefHelper.getUserAccessToken()}")
                }

                return@Interceptor chain.proceed(newRequest.build())
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cache(null)
                .build()

            val cloudURl = if(SharedPrefHelper.getDeveloperOptions().isDeveloperModeEnabled()) {
                SharedPrefHelper.getDeveloperOptions().getDefaultCloud()
            } else {
                DEFAULT_BASE_URL
            }

            LogHelper.debug(TAG, "Using cloud: $cloudURl")

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(cloudURl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CloudAPIService::class.java)
        }

        fun createJSONRequestBody(vararg params: Pair<String, String>): RequestBody =
            JSONObject(mapOf(*params)).toString().toRequestBody(CONTENT_TYPE_JSON)
    }

    @POST(API_LOGIN)
    suspend fun doAuth(@Body params: RequestBody): Response<LoginResponse>

    @POST(API_IMPERSONATE)
    suspend fun doImpersonate(@Body params: RequestBody): Response<LoginResponse>

    @GET(API_CAPTCHA)
    suspend fun getCaptcha(): Response<CaptchaResponse>

    @POST(API_SDA_TOKEN)
    suspend fun getSDAToken(@Body params: RequestBody): Response<SDATokenResponse>

    @GET(API_USER_ME)
    suspend fun getUserProfile(): Response<UserProfile>

    @GET(API_ACCOUNTS_ME)
    suspend fun getAccountProfile(): Response<AccountProfile>

    @GET(API_ALL_WORKFLOWS)
    suspend fun getAssignedWorkflows(
        @Query(KEY_LIMIT) itemsPerPage: Int,
        @Query(KEY_ASSIGNEE_ID) assignee: String,
        @Query(KEY_AFTER_ID) after: String? = null
    ): Response<WorkflowsResponse>

    @POST("$API_ASSIGNED_WORKFLOWS/{$KEY_WORKFLOW_ID}$API_WORKFLOW_SYNC")
    suspend fun syncWorkflow(
        @Path(KEY_WORKFLOW_ID) workflowID: String
    ): Response<ResponseBody>

    @Streaming
    @GET("$API_WORKFLOW_FILES/{$KEY_FILE_ID}")
    suspend fun getWorkflowTaskAssetFile(
        @Path(KEY_FILE_ID) fileID: String
    ): Response<ResponseBody>

    @Multipart
    @POST(API_WORKFLOW_FILES)
    suspend fun uploadWorkflowTaskAssetFile(
        @Part filePart: MultipartBody.Part
    ): Response<FileUploadResponse>

    @POST(API_WORKFLOW_DEVICE_RUNS)
    suspend fun uploadRunLogs(
        @Body params: RequestBody
    ): Response<DeviceRunUploadResponse>

    @GET("$API_ACCOUNTS/{$KEY_ACCOUNT_ID}$API_BRANDING_IMAGES/{$KEY_THEME}")
    suspend fun getAccountBrandingImages(
        @Path(KEY_ACCOUNT_ID) accountID: String,
        @Path(KEY_THEME) theme: String
    ): Response<BrandingImageResponse>

    @GET("$API_ACCOUNTS/{$KEY_ACCOUNT_ID}$API_BRANDING_COLORS/{$KEY_THEME}")
    suspend fun getAccountBrandingColors(
        @Path(KEY_ACCOUNT_ID) accountID: String,
        @Path(KEY_THEME) theme: String
    ): Response<ResponseBody>

    @GET(API_DEVICES)
    suspend fun getDevices(
        @Query(KEY_LIMIT) itemsPerPage: Int,
        @Query(KEY_FILTER) filter: String,
        @Query(KEY_ORDER) order: String,
        @Query(KEY_AFTER_ID) after: String? = null
    ): Response<IoTDevicesResponse>

    @GET(API_DEVICE_ENROLLMENTS)
    suspend fun getEnrollingDevices(
        @Query(KEY_LIMIT) itemsPerPage: Int,
        @Query(KEY_FILTER) filter: String,
        @Query(KEY_ORDER) order: String,
        @Query(KEY_AFTER_ID) after: String? = null
    ): Response<EnrollingIoTDevicesResponse>

    @POST(API_DEVICE_ENROLLMENTS)
    suspend fun enrollDevice(
        @Body params: RequestBody
    ): Response<EnrollingIoTDevice>

}