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

package com.arm.peliondevicemanagement.components.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProfileModel(
    @SerializedName("id")
    val userID: String,
    @SerializedName("full_name")
    val userName: String,
    @SerializedName("email")
    val userEmail: String,
    @SerializedName("is_totp_enabled")
    val isMultiAuthEnabled: Boolean,
    @SerializedName("account_id")
    val accountID: String,
    @SerializedName("last_login_time")
    val userLastLoginTime: Long,
    @SerializedName("login_history")
    val loginHistory: List<LoginHistoryModel>,
    val status: String
): Parcelable