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

package com.arm.peliondevicemanagement

import android.app.Application
import android.content.res.Configuration
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.api.CloudAPIService
import com.arm.peliondevicemanagement.services.cache.WorkflowDB

class AppController : Application() {

    companion object {
        private val TAG: String = AppController::class.java.simpleName
        internal var appController: AppController? = null
        private var cloudRepository: CloudRepository? = null
        private var cloudAPIService: CloudAPIService? = null
        private var workflowDB: WorkflowDB? = null

        internal fun getCloudRepository(): CloudRepository = cloudRepository!!
        internal fun getWorkflowDB(): WorkflowDB = workflowDB!!
    }

    override fun onCreate() {
        super.onCreate()
        LogHelper.debug(TAG, "onApplicationCreate()")

        appController = this
        cloudAPIService = CloudAPIService()
        cloudRepository = CloudRepository(cloudAPIService!!)
        workflowDB = WorkflowDB.getInstance(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LogHelper.debug(TAG, "onConfigurationChanged()")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LogHelper.debug(TAG, "onLowMemory()")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        LogHelper.debug(TAG, "onTrimMemory()")
    }

    override fun onTerminate() {
        super.onTerminate()
        LogHelper.debug(TAG, "onTerminate()")
    }
}