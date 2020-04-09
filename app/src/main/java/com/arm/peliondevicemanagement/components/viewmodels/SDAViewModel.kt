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
import com.arm.mbed.sda.proxysdk.SecuredDeviceAccess
import com.arm.peliondevicemanagement.components.models.workflow.DeviceResponseModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_VERIFY
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.transport.ble.ArmBleDevice
import com.arm.peliondevicemanagement.transport.sda.DeviceCommand
import com.arm.pelionmobiletransportsdk.ble.BleDevice
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

    private var deviceLock: Boolean = false
    private var device: ArmBleDevice? = null
    private val bleConnectionCallback = object: ArmBleDevice.ArmBleConnectionCallback {
        override fun onDisconnect() {
            deviceStateLiveData.postValue(DEVICE_STATE_DISCONNECTED)
        }
    }

    private var jobID: String? = null
    private var accessToken: String? = null
    private var jobCommands: List<DeviceCommand>? = null

    val deviceStateLiveData = MutableLiveData<String>()
    val responseLiveData = MutableLiveData<DeviceResponseModel>()

    private fun connectToDevice(context: Context, deviceMAC: String) {
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
                deviceLock = false
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
            }
        }
    }

    fun fetchDeviceEndpoint() {
        scope.launch {
            deviceStateLiveData.postValue(DEVICE_STATE_VERIFY)
            val deviceEndpoint = device!!.readEndpoint()
            responseLiveData.postValue(
                DeviceResponseModel("endpoint:$deviceEndpoint", null))
        }
    }

    fun connectDevices(context: Context,
                       devices: List<BleDevice>,
                       workflowID: String,
                       sdaToken: String,
                       commands: List<DeviceCommand>){
        scope.launch {
            jobID = workflowID
            accessToken = sdaToken
            jobCommands = commands
            devices.forEach { device ->
                LogHelper.debug(TAG, "Connecting device-> Name: ${device.deviceName}, " +
                        "MAC: ${device.deviceAddress}, RSSI: ${device.deviceRSSI}")
                // Now connect to the device
                deviceLock = true
                connectToDevice(context, device.deviceAddress)
                while (deviceLock){
                    // Wait for the operation to complete
                }
                LogHelper.debug(TAG, "DeviceLock released, now moving to next device")
            }
            // All operations are completed
            //jobID = null
            //accessToken = null
            //jobCommands = null
        }
    }

    fun runJob(){
        LogHelper.debug(TAG, "runJob() Let's do last stage verifications")
        // Stage 1
        if(accessToken != null){
            LogHelper.debug(TAG, "AccessToken->OK")
        } else {
            LogHelper.debug(TAG, "AccessToken->N/A")
            deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
            responseLiveData.postValue(DeviceResponseModel("sda", null))
            return
        }
        // Stage 2
        if(!jobCommands.isNullOrEmpty()){
            LogHelper.debug(TAG, "DeviceCommands->OK, Total: ${jobCommands?.size}, content: $jobCommands")
        } else {
            LogHelper.debug(TAG, "DeviceCommands->N/A")
            deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
            responseLiveData.postValue(DeviceResponseModel("sda", null))
            return
        }
        // All Done, now RUNNNN
        LogHelper.debug(TAG, "All Done, now RUNNNN")
        //deviceStateLiveData.postValue(DEVICE_STATE_COMPLETED)
        //responseLiveData.postValue(DeviceResponseModel("sda", null))
        sendMessageToDevice(jobCommands!![0])
    }

    fun sendMessageToDevice(deviceCommand: DeviceCommand) {
        var deviceResponse: DeviceResponseModel
        scope.launch {
            try {
                deviceStateLiveData.postValue(DEVICE_STATE_RUNNING)
                val opResponse = SecuredDeviceAccess.sendMessage(accessToken,
                    deviceCommand.command, deviceCommand.commandParams, device)
                if(opResponse != null){
                    deviceStateLiveData.postValue(DEVICE_STATE_COMPLETED)
                } else {
                    deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
                }
                deviceResponse = DeviceResponseModel("sda", opResponse)
                responseLiveData.postValue(deviceResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DEVICE_STATE_FAILED)
                deviceResponse = DeviceResponseModel("sda", null)
                responseLiveData.postValue(deviceResponse)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}