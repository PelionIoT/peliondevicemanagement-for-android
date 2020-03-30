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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arm.mbed.sda.proxysdk.SecuredDeviceAccess
import com.arm.mbed.sda.proxysdk.protocol.OperationResponse
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.transport.ble.ArmBleDevice
import com.arm.peliondevicemanagement.transport.sda.DeviceCommand
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class SDAViewModel : ViewModel() {

    companion object {
        private val TAG: String = SDAViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)

    private var device: ArmBleDevice? = null
    private val bleConnectionCallback = object: ArmBleDevice.ArmBleConnectionCallback {
        override fun onDisconnect() {
            deviceStateLiveData.postValue(DEVICE_STATE_DISCONNECTED)
        }
    }

    val deviceStateLiveData = MutableLiveData<String>()
    val opResponseLiveData = MutableLiveData<OperationResponse>()

    fun connectToDevice(context: Context, deviceMAC: String) {
        scope.launch {
            try {
                deviceStateLiveData.postValue(DEVICE_STATE_CONNECTING)
                device = ArmBleDevice(context, deviceMAC)
                if(device!!.connect(bleConnectionCallback)){
                    delay(20)
                    if(device!!.requestHigherMtu(ArmBleDevice.MTU_SIZE)){
                        deviceStateLiveData.postValue(DEVICE_STATE_CONNECTED)
                    } else {
                        device = null
                        deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
                    }
                } else {
                    device = null
                    deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
                }
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
            }
        }
    }

    fun disconnectFromDevice() {
        scope.launch {
            try {
                delay(20)
                device!!.disconnect()
                device = null
                deviceStateLiveData.postValue(DEVICE_STATE_DISCONNECTED)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
            }
        }
    }

    fun sendMessageToDevice(deviceCommand: DeviceCommand) {
        scope.launch {
            try {
                deviceStateLiveData.postValue(DEVICE_STATE_RUNNING)
                val sdaToken = SharedPrefHelper.getSDAToken()
                val opResponse = SecuredDeviceAccess.sendMessage(sdaToken,
                    deviceCommand.command, deviceCommand.commandParams, device)
                if(opResponse != null){
                    deviceStateLiveData.postValue(DEVICE_STATE_COMPLETED)
                } else {
                    deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
                }
                opResponseLiveData.postValue(opResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                opResponseLiveData.postValue(null)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}