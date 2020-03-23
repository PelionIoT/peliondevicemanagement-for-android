package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import com.arm.peliondevicemanagement.components.models.workflow.TaskParamModel
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorkflowTaskModel(
    @SerializedName("id")
    val taskID: String,
    @SerializedName("name")
    val taskName: String,
    @SerializedName("description")
    val taskDescription: String,
    @SerializedName("mandatory")
    val isMandatory: Boolean,
    @SerializedName("input_params")
    val inputParameters: List<TaskParamModel>,
    @SerializedName("output_params")
    val outputParameters: List<TaskParamModel>
): Parcelable