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

package com.arm.peliondevicemanagement.constants

object SharedPrefConstants {

    // Preferences File
    const val SHARED_PREF_FILENAME = "pdmSharedPrefs"

    // Credentials
    const val STORE_USER_NAME = "userName"
    const val STORE_USER_PASSWORD = "userPassword"
    const val STORE_USER_ACCESS_TOKEN = "userAccessToken"
    const val STORE_SELECTED_USER_ID = "selectedUserId"

    // Profile
    const val STORE_USER_PROFILE = "profile"
    const val STORE_USER_ACCOUNT_PROFILE = "accountProfile"

    // Accounts
    const val STORE_ACCOUNTS = "accounts"
    const val STORE_SUPPORTS_MULTI_ACCOUNTS = "multiAccounts"
    const val STORE_SELECTED_ACCOUNT_ID = "selectedAccountId"
    const val STORE_SELECTED_ACCOUNT_NAME = "selectedAccountName"

    // 2-Factor Auth
    const val STORE_CAPTCHA_STATUS = "captcha"
    const val STORE_OTP_STATUS = "otp_required"
    const val STORE_OTP_VALUE = "otp_value"

    // Theme
    const val STORE_DARK_THEME_STATUS = "darkThemeStatus"

    // Feature Flags
    const val STORE_WORKFLOW_SERVICE_STATUS = "workflowEnabled"

    // SDA Flags
    const val STORE_SDA_POPPEMPUB_KEY = "sdaPopPemPubKey"
    const val STORE_SDA_EXECUTION_MODE = "sdaExecutionMode"

}