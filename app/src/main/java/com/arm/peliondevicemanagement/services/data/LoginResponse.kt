package com.arm.peliondevicemanagement.services.data

import com.arm.peliondevicemanagement.components.models.AccountModel
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val accounts: List<AccountModel>,
    @SerializedName("account_id")
    val accountID: String,
    @SerializedName("user_id")
    val emailID: String,
    @SerializedName("token")
    val accessToken: String,
    @SerializedName("expires_in")
    val accessTokenExpiresIn: Int,
    val role: String,
    val status: String,
    @SerializedName("mfa_status")
    val twoFactorAuthStatus: String
)