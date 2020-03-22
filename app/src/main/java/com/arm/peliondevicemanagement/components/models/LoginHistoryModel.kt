package com.arm.peliondevicemanagement.components.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LoginHistoryModel(
    val date: String,
    @SerializedName("success")
    val status: Boolean,
    @SerializedName("ip_address")
    val ipAddress: String,
    @SerializedName("user_agent")
    val userAgent: String
): Parcelable