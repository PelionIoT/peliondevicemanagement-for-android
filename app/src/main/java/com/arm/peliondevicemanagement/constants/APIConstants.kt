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

    // Auth Endpoints
    const val API_LOGIN = "/auth/login"
    const val API_IMPERSONATE = "/auth/impersonate"
    const val API_CAPTCHA = "/auth/captcha"
    const val API_USER_ME = "/v3/users/me"
    const val API_ACCOUNTS_ME = "/v3/accounts/me?include=policies"

    // Workflow Service Endpoints
    const val API_WORKFLOWS = "/v3/users/me/pdm-workflows"
    const val API_WORKFLOW_SYNC = "/sync"
    const val API_WORKFLOW_FILES = "/v3/pdm-workflow-files"
    const val API_WORKFLOW_DEVICE_RUNS = "/v3/pdm-workflow-device-runs"

    // Theme Branding Endpoints
    const val API_BRANDING_COLORS_LIGHT = "/branding-colors/light"
    const val API_BRANDING_COLORS_DARK = "/branding-colors/dark"
    const val API_BRANDING_IMAGES_LIGHT = "/branding-images/light"
    const val API_BRANDING_IMAGES_DARK = "/branding-images/dark"

    // Endpoint Keys
    const val KEY_AUTHORIZATION = "Authorization"
    const val KEY_CONTENT_TYPE = "Content-Type"
    const val KEY_CONTENT_TYPE_JSON = "application/json"
    const val KEY_BEARER = "Bearer"
    const val KEY_USERNAME = "username"
    const val KEY_PASSWORD = "password"
    const val KEY_GRANT_TYPE = "grant_type"
    const val KEY_ACCESS_TOKEN = "token"
    const val KEY_CAPTCHA_ID = "captcha_id"
    const val KEY_CAPTCHA = "captcha"
    const val KEY_OTP_TOKEN = "otp"
    const val KEY_ACCOUNT = "account"
    const val KEY_ACCOUNTS = "accounts"
    const val KEY_ACCOUNT_ID = "account_id"

}