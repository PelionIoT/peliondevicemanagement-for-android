package com.arm.peliondevicemanagement.components.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProfileModel(
    @SerializedName("id")
    val userID: String,
    @SerializedName("full_name")
    val userName: String,
    @SerializedName("email")
    val userEmail: String,
    @SerializedName("is_totp_enabled")
    val isMultiAuthEnabled: Boolean,
    @SerializedName("account_id")
    val accountID: String,
    @SerializedName("last_login_time")
    val userLastLoginTime: Long,
    @SerializedName("login_history")
    val loginHistory: List<LoginHistoryModel>,
    val status: String
): Parcelable