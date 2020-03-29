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