package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorkflowDeviceModel(
    val deviceName: String,
    val deviceState: String
): Parcelable