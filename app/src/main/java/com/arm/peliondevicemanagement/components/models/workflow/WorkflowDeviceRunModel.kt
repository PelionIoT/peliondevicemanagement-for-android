package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorkflowDeviceRunModel(
    val jobID: String,
    val jobName: String,
    val jobStatus: String,
    val jobTasks: List<WorkflowTaskModel>,
    val jobDevices: List<WorkflowDeviceModel>,
    val jobSDAToken: String
): Parcelable