/*
 * Copyright 2020 ARM Ltd.
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

package com.arm.peliondevicemanagement.components.models.workflow.device

import android.os.Parcelable
import com.arm.peliondevicemanagement.components.models.workflow.task.TaskRun
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DeviceRunLogs(
    @SerializedName("workflow_id") val workflowID: String,
    @SerializedName("device_id") val deviceID: String,
    @SerializedName("status") var deviceStatus: String,
    @SerializedName("location") val deviceLocation: String,
    @SerializedName("log") val deviceLog: String,
    @SerializedName("execution_time") val deviceExecutionTime: String,
    @SerializedName("task_runs") val deviceTaskRuns: ArrayList<TaskRun>
): Parcelable