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

package com.arm.peliondevicemanagement.helpers

import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.constants.SharedPrefConstants
import com.arm.peliondevicemanagement.utils.SharedPrefManager

object SharedPrefHelper {

    private val developerOptions = DeveloperOptionsHelper

    // API to access developer-options storage
    internal fun getDeveloperOptions() = developerOptions

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

    internal fun getSelectedUserID(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_USER_ID, "")!!

    internal fun getStoredUserProfile(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_PROFILE, "")!!

    internal fun getStoredAccountProfile(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_USER_ACCOUNT_PROFILE, "")!!

    internal fun getStoredAccounts(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_ACCOUNTS, "")!!

    internal fun getSelectedAccountID(): String =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID, "")!!

    internal fun getSelectedAccountName(): String? =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SELECTED_ACCOUNT_NAME, "")!!

    internal fun getSDAPopPemPubKey(): String =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SDA_POPPEMPUB_KEY, "")!!

    internal fun isMultiAccountSupported(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_SUPPORTS_MULTI_ACCOUNTS, false)

    internal fun isDarkThemeEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DARK_THEME_STATUS, false)

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

    internal fun storeSelectedUserID(userID: String?) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_SELECTED_USER_ID, userID)
            .apply()

    internal fun storeUserProfile(accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_PROFILE, accessToken)
            .apply()

    internal fun storeUserAccountProfile(accessToken: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_USER_ACCOUNT_PROFILE, accessToken)
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

    internal fun storeSDAPopPemPubKey(popPemPubKey: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_SDA_POPPEMPUB_KEY, popPemPubKey)
            .apply()

    internal fun setDarkThemeStatus(enabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DARK_THEME_STATUS, enabled)
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

    internal fun removeAccessToken(){
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_USER_ACCESS_TOKEN)
        editor.apply()
    }

    internal fun clearEverything() {
        // Reset developer-options
        developerOptions.resetOptions()
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_USER_NAME)
        editor.remove(SharedPrefConstants.STORE_USER_PASSWORD)
        editor.remove(SharedPrefConstants.STORE_USER_ACCESS_TOKEN)
        editor.remove(SharedPrefConstants.STORE_SELECTED_USER_ID)
        editor.remove(SharedPrefConstants.STORE_ACCOUNTS)
        editor.remove(SharedPrefConstants.STORE_USER_PROFILE)
        editor.remove(SharedPrefConstants.STORE_USER_ACCOUNT_PROFILE)
        editor.remove(SharedPrefConstants.STORE_SELECTED_ACCOUNT_ID)
        editor.remove(SharedPrefConstants.STORE_SELECTED_ACCOUNT_NAME)
        editor.remove(SharedPrefConstants.STORE_SDA_POPPEMPUB_KEY)
        editor.apply()
    }
}