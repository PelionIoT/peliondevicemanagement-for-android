package com.arm.peliondevicemanagement.services.data

import com.google.gson.annotations.SerializedName

data class UserAccount(
    val id: String,
    val alias: String,
    @SerializedName("display_name")
    val displayName: String,
    val status: String
)