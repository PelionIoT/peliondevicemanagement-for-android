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

package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.LicenseModel
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseErrorResponseFromJson
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LicensesViewModel : ViewModel() {

    companion object {
        private val TAG: String = LicensesViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val repository: CloudRepository = AppController.getCloudRepository()

    private val _licensesLiveData = MutableLiveData<List<LicenseModel>>()
    private val _errorResponseLiveData = MutableLiveData<ErrorResponse>()

    fun getLicensesLiveData(): LiveData<List<LicenseModel>> = _licensesLiveData
    fun getErrorResponseLiveData(): LiveData<ErrorResponse> = _errorResponseLiveData

    fun loadLicensesFromCloud() {
        scope.launch {
            try {
                val licensesResponse = repository.getLicenses()
                _licensesLiveData.postValue(licensesResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}