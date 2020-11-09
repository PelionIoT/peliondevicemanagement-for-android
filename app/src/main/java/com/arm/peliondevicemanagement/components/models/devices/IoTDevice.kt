/*
 * Copyright 2020 ARM Ltd.
 *
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

package com.arm.peliondevicemanagement.components.models.devices

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class IoTDevice(
    @SerializedName("id") val deviceID: String,
    @SerializedName("name") val deviceName: String,
    @SerializedName("vendor_id") val vendorID: String,
    @SerializedName("endpoint_name") val endpointName: String,
    @SerializedName("state") val currentState: String,
    @SerializedName("deployed_state") val deployedState: String,
    @SerializedName("host_gateway") val hostGateway: String,
    @SerializedName("created_at") val createdAt: String
): Parcelable