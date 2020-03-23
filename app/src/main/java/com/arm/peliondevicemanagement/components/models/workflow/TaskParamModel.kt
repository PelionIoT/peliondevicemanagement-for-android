package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TaskParamModel(
    @SerializedName("name")
    val paramName: String,
    @SerializedName("type")
    val paramType: String,
    @SerializedName("value")
    val paramValue: String,
    @SerializedName("mandatory")
    val isMandatory: Boolean
): Parcelable