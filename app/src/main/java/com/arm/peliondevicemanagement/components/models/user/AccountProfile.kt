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

package com.arm.peliondevicemanagement.components.models.user

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AccountProfileModel(
    @SerializedName("id") val accountID: String,
    @SerializedName("display_name") val accountName: String,
    @SerializedName("email") val accountEmail: String,
    @SerializedName("company") val companyName: String,
    @SerializedName("custom_fields") val customFlags: CustomFlagsModel
    // For later-use, when dealing with multiple features
    //@SerializedName("policies") val accountPolicies: List<AccountPolicyModel>
): Parcelable

@Parcelize
data class CustomFlagsModel(
    @SerializedName("default_theme") val defaultTheme: String
): Parcelable