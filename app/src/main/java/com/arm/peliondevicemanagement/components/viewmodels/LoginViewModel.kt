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
import com.arm.peliondevicemanagement.components.models.user.AccountProfileModel
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.data.LoginResponse
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LoginViewModel : ViewModel() {

    companion object {
        private val TAG: String = LoginViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val cloudRepository: CloudRepository = AppController.getCloudRepository()

    private val userAccountLiveData = MutableLiveData<LoginResponse>()
    private val userProfileLiveData = MutableLiveData<UserProfile>()
    private val userAccountProfileLiveData = MutableLiveData<AccountProfileModel>()

    // LiveData APIs
    fun getLoginActionLiveData(): LiveData<LoginResponse> = userAccountLiveData
    fun getUserProfileLiveData(): LiveData<UserProfile> = userProfileLiveData
    fun getAccountProfileLiveData(): LiveData<AccountProfileModel> = userAccountProfileLiveData

    // GET & POST APIs
    fun doLogin(username: String, password: String, accountID: String = "") {
        scope.launch {
            try {
                val userAccountResponse = cloudRepository.doAuth(username, password, accountID)
                userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userAccountLiveData.postValue(null)
            }
        }
    }

    fun doImpersonate(accountID: String) {
        scope.launch {
            try {
                val userAccountResponse = cloudRepository.doImpersonate(accountID)
                userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userAccountLiveData.postValue(null)
            }
        }
    }

    fun fetchUserProfile() {
        scope.launch {
            try {
                val userProfileResponse = cloudRepository.getUserProfile()
                userProfileLiveData.postValue(userProfileResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userProfileLiveData.postValue(null)
            }
        }
    }

    fun fetchAccountProfile() {
        scope.launch {
            try {
                val userAccountProfileResponse = cloudRepository.getAccountProfile()
                userAccountProfileLiveData.postValue(userAccountProfileResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userAccountProfileLiveData.postValue(null)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}