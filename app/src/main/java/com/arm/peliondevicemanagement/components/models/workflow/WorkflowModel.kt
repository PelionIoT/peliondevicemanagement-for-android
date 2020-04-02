/*
 * Copyright (c) 2018, Arm Limited and affiliates.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.components.models.workflow

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.arm.peliondevicemanagement.helpers.converters.ArrayListConverter
import com.arm.peliondevicemanagement.helpers.converters.ListConverter
import com.arm.peliondevicemanagement.helpers.converters.StringArrayConverter
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize @Entity(tableName = "workflows")
@TypeConverters(
    StringArrayConverter::class,
    ArrayListConverter::class, ListConverter::class)
data class WorkflowModel(
    @PrimaryKey @field:SerializedName("id") val workflowID: String,
    @field:SerializedName("name") val workflowName: String,
    @field:SerializedName("description") val workflowDescription: String?,
    @field:SerializedName("status") var workflowStatus: String,
    @field:SerializedName("location") val workflowLocation: String,
    @field:SerializedName("aud") val workflowAUDs: Array<String>,
    @field:SerializedName("device") var workflowDevices: ArrayList<WorkflowDeviceModel>?,
    @field:SerializedName("tasks") val workflowTasks: List<WorkflowTaskModel>,
    @field:SerializedName("created_at") val workflowCreatedAt: String,
    @field:SerializedName("execution_time") val workflowExecutedAt: String
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
        if (!workflowAUDs.contentEquals(other.workflowAUDs)) return false
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
        result = 31 * result + workflowAUDs.contentHashCode()
        result = 31 * result + workflowTasks.hashCode()
        result = 31 * result + workflowCreatedAt.hashCode()
        result = 31 * result + workflowExecutedAt.hashCode()
        return result
    }
}