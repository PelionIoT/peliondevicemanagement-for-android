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
import com.arm.peliondevicemanagement.components.models.workflow.DeviceStateResponseModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_VERIFY
import com.arm.peliondevicemanagement.constants.AppConstants.ENDPOINT
import com.arm.peliondevicemanagement.constants.AppConstants.JOB_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.SDA
import com.arm.peliondevicemanagement.constants.DeviceState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.transport.ble.ArmBleDevice
import com.arm.peliondevicemanagement.transport.ble.DumBleDevice
import com.arm.peliondevicemanagement.transport.ble.DummyBleDevice
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
    private var deviceCommandLock: Boolean = false
    // FixME
    //private var bleDevice: ArmBleDevice? = null
    private var bleDevice: DummyBleDevice? = null
    private val bleConnectionCallback = object: ArmBleDevice.ArmBleConnectionCallback {
        override fun onDisconnect() {
            deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.DISCONNECTED, activeDeviceIdentifier))
        }
    }

    private var activeDeviceIdentifier: String = "Unknown Device"

    private var jobID: String? = null
    private var accessToken: String? = null
    private var jobCommands: List<DeviceCommand>? = null

    val deviceStateLiveData = MutableLiveData<DeviceStateResponseModel>()
    val responseLiveData = MutableLiveData<DeviceResponseModel>()

    fun setDeviceCommandLockState(locked: Boolean){
        deviceCommandLock = locked
    }

    private fun connectToDevice(context: Context, deviceMAC: String) {
        scope.launch {
            try {
                activeDeviceIdentifier = deviceMAC
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.CONNECTING, activeDeviceIdentifier))
                //bleDevice = ArmBleDevice(context, deviceMAC)
                // FixME
                bleDevice = DummyBleDevice(context, deviceMAC)
                if(bleDevice!!.connect(bleConnectionCallback)){
                    delay(20)
                    // FixME
                    if(bleDevice!!.requestHigherMtu(DummyBleDevice.MTU_SIZE)){
                        deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.CONNECTED, activeDeviceIdentifier))
                    } else {
                        bleDevice = null
                        deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
                    }
                } else {
                    bleDevice = null
                    deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
                }
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
            }
        }
    }

    fun disconnectFromDevice() {
        scope.launch {
            try {
                delay(20)
                bleDevice!!.disconnect()
                bleDevice = null
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.DISCONNECTED, activeDeviceIdentifier))
                deviceLock = false
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
            }
        }
    }

    fun fetchDeviceEndpoint() {
        scope.launch {
            var deviceEndpoint = bleDevice!!.readEndpoint()
            // FixME
            when (activeDeviceIdentifier) {
                "FE:7E:7E:BD:AB:87" -> {
                    deviceEndpoint = "FE:7E:7E:BD:AB:87"
                }
                "XD:7E:7E:BD:AB:70" -> {
                    deviceEndpoint = "026eead293eb926ca57ba92703c00000"
                    activeDeviceIdentifier = deviceEndpoint
                }
                else -> {
                    activeDeviceIdentifier = deviceEndpoint
                }
            }
            deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.VERIFY, activeDeviceIdentifier))
            responseLiveData.postValue(
                DeviceResponseModel("$ENDPOINT:$deviceEndpoint", null))
        }
    }

    // FixME [ devices: List<BleDevice> ]
    fun connectDevices(context: Context,
                       devices: List<DumBleDevice>,
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
            // Signal all-devices completed
            responseLiveData.postValue(DeviceResponseModel(JOB_COMPLETED, null))
        }
    }

    fun runJob(){
        scope.launch {
            LogHelper.debug(TAG, "runJob() Do last-stage verifications")
            // Stage 1 [ Token-check ]
            if(accessToken != null){
                LogHelper.debug(TAG, "AccessToken->OK")
            } else {
                LogHelper.debug(TAG, "AccessToken->N/A")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
                responseLiveData.postValue(DeviceResponseModel("sda", null))
                return@launch
            }
            // Stage 2 [ Command-Check ]
            if(!jobCommands.isNullOrEmpty()){
                LogHelper.debug(TAG, "DeviceCommands->OK, Total: ${jobCommands?.size}")
            } else {
                LogHelper.debug(TAG, "DeviceCommands->N/A")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
                responseLiveData.postValue(DeviceResponseModel("sda", null))
                return@launch
            }
            // Stage 3 [ Showdown Time ]
            LogHelper.debug(TAG, "All Done, now RUNNNN")
            jobCommands?.forEach { deviceCommand ->
                LogHelper.debug(TAG, "Sending command to device ${deviceCommand.command}")
                deviceCommandLock = true
                sendMessageToDevice(deviceCommand)
                while (deviceCommandLock){
                    // Wait for the operation to complete
                }
                LogHelper.debug(TAG, "DeviceCommandLock released, now moving to next command")
            }
            // All commands are now completed
            LogHelper.debug(TAG, "All commands on this device are now complete")
            deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.COMPLETED, activeDeviceIdentifier))
        }
    }

    private fun sendMessageToDevice(deviceCommand: DeviceCommand) {
        var deviceResponse: DeviceResponseModel
        scope.launch {
            try {
                LogHelper.debug(TAG, "sendMessageToDevice() deviceCommand: $deviceCommand")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.RUNNING, activeDeviceIdentifier))
                val opResponse = SecuredDeviceAccess.sendMessage(accessToken,
                    deviceCommand.command, deviceCommand.commandParams, bleDevice)
                if(opResponse != null){
                    deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.COMMAND_COMPLETED, activeDeviceIdentifier))
                } else {
                    deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.COMMAND_FAILED, activeDeviceIdentifier))
                }
                deviceResponse = DeviceResponseModel(SDA, opResponse)
                responseLiveData.postValue(deviceResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DeviceStateResponseModel(DeviceState.FAILED, activeDeviceIdentifier))
                deviceResponse = DeviceResponseModel(SDA, null)
                responseLiveData.postValue(deviceResponse)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}