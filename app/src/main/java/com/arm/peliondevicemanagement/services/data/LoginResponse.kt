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

package com.arm.peliondevicemanagement.services.data

import com.arm.peliondevicemanagement.components.models.user.Account
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val accounts: List<Account>,
    @SerializedName("account_id")
    val accountID: String,
    @SerializedName("user_id")
    val emailID: String,
    @SerializedName("token")
    val accessToken: String,
    @SerializedName("expires_in")
    val accessTokenExpiresIn: Int,
    val role: String,
    val status: String,
    @SerializedName("mfa_status")
    val twoFactorAuthStatus: String
)