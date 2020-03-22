package com.arm.peliondevicemanagement.components.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AccountModel(
    @SerializedName("id")
    val accountID: String,
    val alias: String,
    @SerializedName("display_name")
    val accountName: String,
    val status: String,
    var isSelected: Boolean = false
): Parcelable