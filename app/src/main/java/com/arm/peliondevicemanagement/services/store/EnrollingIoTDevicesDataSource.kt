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

package com.arm.peliondevicemanagement.services.store

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ACCOUNT_ID
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_DESCENDING
import com.arm.peliondevicemanagement.constants.AppConstants.NETWORK_PAGE_SIZE
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.data.EnrollingIoTDevicesResponse
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.utils.PlatformUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EnrollingIoTDevicesDataSource(
    private val scope: CoroutineScope,
    private val cloudRepository: CloudRepository,
    private val stateLiveData: MutableLiveData<LoadState>,
    private val searchIndex: ArrayList<EnrollingIoTDevice>
): PageKeyedDataSource<String, EnrollingIoTDevice>()  {

    companion object {
        private val TAG: String = EnrollingIoTDevicesDataSource::class.java.simpleName
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, EnrollingIoTDevice>
    ) {
        scope.launch {
            requestAndLoadDevices { deviceResponse ->
                if(deviceResponse != null){
                    val deviceList = deviceResponse.enrollingDevices
                    val lastItem = deviceList.last()
                    LogHelper.debug(TAG, "loadInitial() loadedItems: ${deviceList.size}, " +
                            "afterID: ${lastItem.deviceID}")
                    searchIndex.clear()
                    searchIndex.addAll(deviceList)
                    callback.onResult(deviceList, null, lastItem.deviceID)
                } else {
                    LogHelper.debug(TAG, "loadInitial() No more items available")
                    callback.onResult(listOf(), null, null)
                }
            }
        }
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, EnrollingIoTDevice>
    ) {
        LogHelper.debug(TAG, "loadAfter() afterID: ${params.key}")
        scope.launch {
            requestAndLoadDevices(params.key) { deviceResponse ->
                if(deviceResponse != null){
                    val deviceList = deviceResponse.enrollingDevices
                    val lastItem = deviceList.last()
                    LogHelper.debug(TAG, "loadAfter() loadedItems: ${deviceList.size}, " +
                            "afterID: ${lastItem.deviceID}")
                    searchIndex.addAll(deviceList)
                    callback.onResult(deviceList, lastItem.deviceID)
                } else {
                    LogHelper.debug(TAG, "loadAfter() No more items available")
                    callback.onResult(listOf(), params.key)
                }
            }
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, EnrollingIoTDevice>
    ) {
        // Not needed
    }

    private suspend fun requestAndLoadDevices(after: String? = null, loadFinished: (EnrollingIoTDevicesResponse?) -> Unit) {
        val accountID = "$KEY_ACCOUNT_ID%3D${SharedPrefHelper.getSelectedAccountID()}"
        try {
            val response = if(after != null){
                cloudRepository.getEnrollingDevices(NETWORK_PAGE_SIZE, accountID, KEY_DESCENDING, after)
            } else {
                cloudRepository.getEnrollingDevices(NETWORK_PAGE_SIZE, accountID, KEY_DESCENDING)
            }

            if(response?.enrollingDevices!!.isNotEmpty()){
                stateLiveData.postValue(LoadState.DOWNLOADED)
                loadFinished(response)
            } else {
                LogHelper.debug(TAG, "Network data-unavailable")
                stateLiveData.postValue(LoadState.FAILED)
                loadFinished(null)
            }

        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
            if(errorResponse != null){
                stateLiveData.postValue(LoadState.UNAUTHORIZED)
            } else {
                stateLiveData.postValue(LoadState.NO_NETWORK)
            }
            loadFinished(null)
        }
    }
}