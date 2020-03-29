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
import retrofit2.Response
import java.io.IOException

open class BaseRepository{

    suspend fun <T : Any> doSafeAPIRequest(call: suspend () -> Response<T>, errorMessage: String): T? {
        val result : Result<T> = returnSafeAPIResponse(call,errorMessage)
        var data : T? = null

        when(result) {
            is Result.Success ->
                data = result.data
            is Result.Error -> {
                LogHelper.debug("RepositoryError", "Exception - ${result.exception}")
            }
        }
        return data
    }

    private suspend fun <T: Any> returnSafeAPIResponse(call: suspend ()-> Response<T>, errorMessage: String) : Result<T>{
        val response = call.invoke()
        if(response.isSuccessful) return Result.Success(response.body()!!)

        return Result.Error(IOException("ERROR- $errorMessage"))
    }
}