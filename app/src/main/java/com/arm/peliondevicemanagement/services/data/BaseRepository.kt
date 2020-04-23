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

package com.arm.peliondevicemanagement.services.data

import com.arm.peliondevicemanagement.helpers.LogHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response

open class BaseRepository{

    suspend fun <T : Any> doSafeAPIRequest(call: suspend () -> Response<T>): T? {
        val result : Result<T> = returnSafeAPIResponse(call)
        var data : T? = null

        when(result) {
            is Result.Success ->
                data = result.data
            is Result.Error -> {
                throw Exception(result.error)
            }
        }
        return data
    }

    private suspend fun <T: Any> returnSafeAPIResponse(call: suspend ()-> Response<T>) : Result<T>{
        // Call network-request
        val response = call.invoke()

        // Print to console
        /*LogHelper.debug("returnSafeAPIResponse()", "success: ${response.isSuccessful},\n" +
                "code: ${response.code()}, message: ${response.message()},\n" +
                "body: ${response.body()}")*/

        if(response.isSuccessful) return Result.Success(response.body()!!)

        // Parse error-response
        val gson = Gson()
        val type = object : TypeToken<ErrorResponse>() {}.type
        val errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)

        if(errorResponse?.errorCode == 0 && response.code() == 400){
            errorResponse.errorCode = 400
            errorResponse.errorType = "bad_request"
            errorResponse.errorMessage = "Invalid request"
        }
        // Convert it to JSON to be parsed later
        val error = gson.toJson(errorResponse, type)
        // Now return error
        return Result.Error(error)
    }
}