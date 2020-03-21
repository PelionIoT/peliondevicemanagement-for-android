package com.arm.peliondevicemanagement.services.data

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("object")
    val errorObject: String,
    @SerializedName("code")
    val errorCode: Int,
    @SerializedName("type")
    val errorType: String,
    @SerializedName("message")
    val errorMessage: String
)