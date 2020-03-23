package com.arm.peliondevicemanagement.services.data

import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.google.gson.annotations.SerializedName

data class WorkflowsResponse(
    @SerializedName("data")
    val workflows: List<WorkflowModel>
)