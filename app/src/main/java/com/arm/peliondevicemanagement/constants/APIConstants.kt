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

package com.arm.peliondevicemanagement.constants

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object APIConstants {

    // Cloud URLs
    private const val CLOUD_URL_PRODUCTION = "api.us-east-1.mbedcloud.com"
    private const val CLOUD_URL_STAGING = "api-os2.mbedcloudstaging.net"
    private const val CLOUD_URL_INTEGRATION = "lab-api.mbedcloudintegration.net"

    // Default Base URL
    private const val DEFAULT_SSL = "https://"
    const val DEFAULT_BASE_URL = DEFAULT_SSL + CLOUD_URL_PRODUCTION

    val CONTENT_TYPE_JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
    val CONTENT_TYPE_TEXT_PLAIN: MediaType = "text/plain".toMediaTypeOrNull()!!

    // Auth Endpoints
    const val API_LOGIN = "/auth/login"
    const val API_IMPERSONATE = "/auth/impersonate"
    const val API_CAPTCHA = "/auth/captcha"
    const val API_SDA_TOKEN = "/ace-auth/token"
    const val API_USER_ME = "/v3/users/me"
    const val API_ACCOUNTS = "/v3/accounts"
    const val API_ACCOUNTS_ME = "$API_ACCOUNTS/me?include=policies"

    // Workflow Service Endpoints
    const val API_ALL_WORKFLOWS = "/v3/pdm-workflows"
    const val API_ASSIGNED_WORKFLOWS = "/v3/users/me/pdm-workflows"
    const val API_WORKFLOW_SYNC = "/sync"
    const val API_WORKFLOW_FILES = "/v3/pdm-workflow-files"
    const val API_WORKFLOW_DEVICE_RUNS = "/v3/pdm-workflow-device-runs"

    // Theme Branding Endpoints
    const val API_BRANDING_COLORS = "/branding-colors"
    const val API_BRANDING_IMAGES = "/branding-images"

    // Device Management Service Endpoints
    const val API_DEVICES = "/v3/devices"
    const val API_DEVICE_ENROLLMENTS = "/v3/device-enrollments"

    // Endpoint Keys
    const val KEY_AUTHORIZATION = "Authorization"
    const val KEY_CONTENT_TYPE = "Content-Type"
    const val KEY_CONTENT_TYPE_JSON = "application/json"
    const val KEY_BEARER = "Bearer"
    const val KEY_USERNAME = "username"
    const val KEY_PASSWORD = "password"
    const val KEY_GRANT_TYPE = "grant_type"
    const val KEY_CAPTCHA_ID = "captcha_id"
    const val KEY_CAPTCHA = "captcha"
    const val KEY_OTP_TOKEN = "otp"
    const val KEY_ACCOUNT = "account"
    const val KEY_ACCOUNT_ID = "account_id"
    const val KEY_ERROR_CODE = "error_code"
    const val KEY_AFTER_ID = "after"
    const val KEY_LIMIT = "limit"
    const val KEY_FILTER = "filter"
    const val KEY_ORDER = "order"
    const val KEY_DESCENDING = "DESC"
    const val KEY_FILE_ID = "file_id"
    const val KEY_WORKFLOW_ID = "workflow_id"
    const val KEY_ASSIGNEE_ID = "assignee__eq"
    const val KEY_THEME = "theme"
    const val KEY_BRAND_LOGO = "brand_logo_landscape"
    const val KEY_VALIDATION_ERROR = "validation_error"
    const val KEY_ENROLLMENT_IDENTITY = "enrollment_identity"

}