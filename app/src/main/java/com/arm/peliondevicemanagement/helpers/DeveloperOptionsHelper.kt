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

object DeveloperOptionsHelper {

    // Developer Options Preferences
    // GET APIs
    internal fun isDeveloperModeEnabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DEVELOPER_MODE_STATUS, false)

    internal fun isReAuthDisabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DISABLE_REAUTH_STATUS, false)

    internal fun getSDAExecutionMode(): String =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getString(SharedPrefConstants.STORE_SDA_EXECUTION_MODE, "")!!

    internal fun isMaxMTUDisabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DISABLE_MAX_MTU_STATUS, false)

    internal fun isJobAutoSyncDisabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DISABLE_JOB_AUTO_SYNC_STATUS, false)

    internal fun isAssetDownloadDisabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DISABLE_ASSET_DOWNLOAD_STATUS, false)

    internal fun isSDATokenDownloadDisabled(): Boolean =
        SharedPrefManager.with(context = AppController.appController!!)!!
            .getBoolean(SharedPrefConstants.STORE_DISABLE_SDA_TOKEN_DOWNLOAD_STATUS, false)

    // SET APIs
    internal fun setDeveloperModeStatus(enabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DEVELOPER_MODE_STATUS, enabled)
            .apply()

    internal fun setReAuthDisabledStatus(disabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DISABLE_REAUTH_STATUS, disabled)
            .apply()

    internal fun storeSDAExecutionMode(mode: String) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putString(SharedPrefConstants.STORE_SDA_EXECUTION_MODE, mode)
            .apply()

    internal fun setMaxMTUDisabledStatus(disabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DISABLE_MAX_MTU_STATUS, disabled)
            .apply()

    internal fun setJobAutoSyncDisabledStatus(disabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DISABLE_JOB_AUTO_SYNC_STATUS, disabled)
            .apply()

    internal fun setAssetDownloadDisabledStatus(disabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DISABLE_ASSET_DOWNLOAD_STATUS, disabled)
            .apply()

    internal fun setSDATokenDownloadDisabledStatus(disabled: Boolean) =
        SharedPrefManager.with(context = AppController.appController!!)!!.edit()
            .putBoolean(SharedPrefConstants.STORE_DISABLE_SDA_TOKEN_DOWNLOAD_STATUS, disabled)
            .apply()

    // DELETE API
    // Clear developer-options
    internal fun resetOptions() {
        val editor = SharedPrefManager.with(context = AppController.appController!!)!!.edit()
        editor.remove(SharedPrefConstants.STORE_DEVELOPER_MODE_STATUS)
        editor.remove(SharedPrefConstants.STORE_DISABLE_REAUTH_STATUS)
        editor.remove(SharedPrefConstants.STORE_SDA_EXECUTION_MODE)
        editor.remove(SharedPrefConstants.STORE_DISABLE_MAX_MTU_STATUS)
        editor.remove(SharedPrefConstants.STORE_DISABLE_JOB_AUTO_SYNC_STATUS)
        editor.remove(SharedPrefConstants.STORE_DISABLE_ASSET_DOWNLOAD_STATUS)
        editor.remove(SharedPrefConstants.STORE_DISABLE_SDA_TOKEN_DOWNLOAD_STATUS)
        editor.apply()
    }

}