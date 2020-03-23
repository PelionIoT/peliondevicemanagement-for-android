package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorkflowModel(
    @SerializedName("id")
    val workflowID: String,
    @SerializedName("name")
    val workflowName: String,
    @SerializedName("description")
    val workflowDescription: String,
    @SerializedName("status")
    val workflowStatus: String,
    @SerializedName("location")
    val workflowLocation: String,
    @SerializedName("aud")
    val workflowDevices: Array<String>,
    @SerializedName("tasks")
    val workflowTasks: List<WorkflowTaskModel>,
    @SerializedName("created_at")
    val workflowCreatedAt: String,
    @SerializedName("execution_time")
    val workflowExecutedAt: String
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkflowModel

        if (workflowID != other.workflowID) return false
        if (workflowName != other.workflowName) return false
        if (workflowDescription != other.workflowDescription) return false
        if (workflowStatus != other.workflowStatus) return false
        if (workflowLocation != other.workflowLocation) return false
        if (!workflowDevices.contentEquals(other.workflowDevices)) return false
        if (workflowTasks != other.workflowTasks) return false
        if (workflowCreatedAt != other.workflowCreatedAt) return false
        if (workflowExecutedAt != other.workflowExecutedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workflowID.hashCode()
        result = 31 * result + workflowName.hashCode()
        result = 31 * result + workflowDescription.hashCode()
        result = 31 * result + workflowStatus.hashCode()
        result = 31 * result + workflowLocation.hashCode()
        result = 31 * result + workflowDevices.contentHashCode()
        result = 31 * result + workflowTasks.hashCode()
        result = 31 * result + workflowCreatedAt.hashCode()
        result = 31 * result + workflowExecutedAt.hashCode()
        return result
    }
}