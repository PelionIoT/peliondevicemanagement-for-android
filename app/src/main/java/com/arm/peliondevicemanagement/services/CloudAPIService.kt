package com.arm.peliondevicemanagement.services

import com.arm.peliondevicemanagement.components.models.ProfileModel
import com.arm.peliondevicemanagement.constants.APIConstants.API_ACCOUNTS_ME
import com.arm.peliondevicemanagement.constants.APIConstants.API_ALL_WORKFLOWS
import com.arm.peliondevicemanagement.services.data.LoginResponse
import com.arm.peliondevicemanagement.constants.APIConstants.API_LOGIN
import com.arm.peliondevicemanagement.constants.APIConstants.API_IMPERSONATE
import com.arm.peliondevicemanagement.constants.APIConstants.API_USER_ME
import com.arm.peliondevicemanagement.constants.APIConstants.API_ASSIGNED_WORKFLOWS
import com.arm.peliondevicemanagement.constants.APIConstants.CONTENT_TYPE_JSON
import com.arm.peliondevicemanagement.constants.APIConstants.DEFAULT_BASE_URL
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_AUTHORIZATION
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_BEARER
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_CONTENT_TYPE
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_CONTENT_TYPE_JSON
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.data.WorkflowsResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CloudAPIService {

    companion object {
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
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(DEFAULT_BASE_URL)
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

    @GET(API_USER_ME)
    suspend fun getProfile(): Response<ProfileModel>

    @GET(API_ACCOUNTS_ME)
    suspend fun getAccountProfile()

    @GET(API_ASSIGNED_WORKFLOWS)
    suspend fun getAssignedWorkflows(): Response<WorkflowsResponse>

    @GET(API_ALL_WORKFLOWS)
    suspend fun getAllWorkflows(): Response<WorkflowsResponse>

}