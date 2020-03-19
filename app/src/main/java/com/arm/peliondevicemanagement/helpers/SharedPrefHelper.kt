package com.arm.peliondevicemanagement.helpers

import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.constants.SharedPrefConstants
import com.arm.peliondevicemanagement.managers.SharedPrefManager

object SharedPrefHelper {

    // Get APIs
    internal fun getUserName(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_NAME, "")!!

    internal fun getUserAccessToken(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_ACCESS_TOKEN, "")!!

    internal fun getStoredAccounts(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_ACCOUNTS, "")!!

    internal fun getSelectedAccountID(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID, "")!!

    internal fun getSelectedAccountName(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_NAME, "")!!

    internal fun isDarkThemeEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DARK_THEME_STATUS, false)

    internal fun isWorkflowServiceEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_WORKFLOW_SERVICE_STATUS, false)


    // POST APIs
    internal fun storeUserName(userName: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_NAME, userName)
            .apply()

    internal fun storeUserAccessToken(accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_ACCESS_TOKEN, accessToken)
            .apply()

    internal fun storeUserCredentials(userName: String, accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_NAME, userName)
            .putString(SharedPrefConstants.STORE_USER_ACCESS_TOKEN, accessToken)
            .apply()

    internal fun storeUserAccounts(accounts: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_ACCOUNTS, accounts)
            .apply()

    internal fun storeSelectedAccountID(accountID: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID, accountID)
            .apply()

    internal fun storeSelectedAccountName(accountName: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_NAME, accountName)
            .apply()

    internal fun setDarkThemeStatus(enabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DARK_THEME_STATUS, enabled)
            .apply()

    internal fun setWorkflowServiceStatus(userName: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_WORKFLOW_SERVICE_STATUS, userName)
            .apply()

    // DELETE APIs
    internal fun clearUserData(removeCredentials: Boolean, removeAccountId: Boolean) {
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()

        editor.remove(SharedPrefConstants.STORE_USER_ACCESS_TOKEN)

        if (removeAccountId)
            editor.remove(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID)

        if (removeCredentials) {
            editor.remove(SharedPrefConstants.STORE_USER_NAME)
        }

        editor.apply()
    }
}