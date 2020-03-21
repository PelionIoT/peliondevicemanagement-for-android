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