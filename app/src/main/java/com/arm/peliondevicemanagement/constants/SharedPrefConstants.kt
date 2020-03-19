package com.arm.peliondevicemanagement.constants

object SharedPrefConstants {

    // Preferences File
    const val SHARED_PREF_FILENAME = "pdmSharedPrefs"

    // Credentials
    const val STORE_USER_NAME = "userName"
    const val STORE_USER_ACCESS_TOKEN = "userAccessToken"

    // Accounts
    const val STORE_ACCOUNTS = "accounts"
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

}