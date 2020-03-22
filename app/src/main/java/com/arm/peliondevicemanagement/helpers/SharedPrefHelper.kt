package com.arm.peliondevicemanagement.helpers

import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.constants.SharedPrefConstants
import com.arm.peliondevicemanagement.managers.SharedPrefManager

object SharedPrefHelper {

    // Get APIs
    internal fun getUserName(): String =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_NAME, "")!!

    internal fun getUserPassword(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_PASSWORD, "")!!

    internal fun getUserAccessToken(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_ACCESS_TOKEN, "")!!

    internal fun getStoredProfile(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_PROFILE, "")!!

    internal fun getStoredAccounts(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_ACCOUNTS, "")!!

    internal fun getSelectedAccountID(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID, "")!!

    internal fun getSelectedAccountName(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_NAME, "")!!

    internal fun isMultiAccountSupported(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_SUPPORTS_MULTI_ACCOUNTS, false)

    internal fun isDarkThemeEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DARK_THEME_STATUS, false)

    internal fun isWorkflowServiceEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_WORKFLOW_SERVICE_STATUS, false)


    // POST APIs
    internal fun storeUserAccessToken(accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_ACCESS_TOKEN, accessToken)
            .apply()

    internal fun storeUserCredentials(userName: String,
                                      userPassword: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_NAME, userName)
            .putString(SharedPrefConstants.STORE_USER_PASSWORD, userPassword)
            .apply()

    internal fun storeUserProfile(accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_PROFILE, accessToken)
            .apply()

    internal fun storeMultiAccountStatus(enabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_SUPPORTS_MULTI_ACCOUNTS, enabled)
            .apply()

    internal fun storeUserAccounts(accounts: String?) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_ACCOUNTS, accounts)
            .apply()

    internal fun storeSelectedAccountID(accountID: String?) =
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
    internal fun removeCredentials(accessTokenAlso: Boolean = false){
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_USER_NAME)
        editor.remove(SharedPrefConstants.STORE_USER_PASSWORD)

        if(accessTokenAlso){
            editor.remove(SharedPrefConstants.STORE_USER_ACCESS_TOKEN)
        }
        editor.apply()
    }

    internal fun removePassword(){
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_USER_PASSWORD)
        editor.apply()
    }

    internal fun clearEverything() {
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_USER_NAME)
        editor.remove(SharedPrefConstants.STORE_USER_PASSWORD)
        editor.remove(SharedPrefConstants.STORE_USER_ACCESS_TOKEN)
        editor.remove(SharedPrefConstants.STORE_ACCOUNTS)
        editor.remove(SharedPrefConstants.STORE_USER_PROFILE)
        editor.apply()
    }
}