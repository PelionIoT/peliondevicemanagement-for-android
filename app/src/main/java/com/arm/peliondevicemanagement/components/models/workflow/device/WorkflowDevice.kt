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
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorkflowDevice(
    val deviceName: String,
    var deviceState: String,
    var deviceRunLogs: DeviceRunLogs? = null
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkflowDevice
        if(deviceName != other.deviceName) return false
        return true
    }

    override fun hashCode(): Int {
        return  deviceName.hashCode()
    }
}