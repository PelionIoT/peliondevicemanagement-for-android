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
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceResponse
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceStateResponse
import com.arm.peliondevicemanagement.components.models.workflow.task.TaskDeviceCommand
import com.arm.peliondevicemanagement.constants.state.DeviceResponseState
import com.arm.peliondevicemanagement.constants.state.DeviceState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.transport.ble.ArmBleDevice
import com.arm.peliondevicemanagement.transport.ble.DumBleDevice
import com.arm.peliondevicemanagement.transport.ble.DummyBleDevice
import com.arm.peliondevicemanagement.transport.sda.DeviceCommand
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class SDAViewModel : ViewModel() {

    companion object {
        private val TAG: String = SDAViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)

    private var processController: Continuation<Boolean>? = null
    // FixME
    //private var bleDevice: ArmBleDevice? = null
    private var bleDevice: DummyBleDevice? = null
    private val bleConnectionCallback = object: ArmBleDevice.ArmBleConnectionCallback {
        override fun onDisconnect() {
            deviceStateLiveData.postValue(
                DeviceStateResponse(
                    DeviceState.DISCONNECTED,
                    activeDeviceIdentifier
                )
            )
        }
    }

    private var activeDeviceIdentifier: String = "Unknown Device"

    private var jobID: String? = null
    private var accessToken: String? = null
    private var jobCommands: List<TaskDeviceCommand>? = null

    val deviceStateLiveData = MutableLiveData<DeviceStateResponse>()
    val responseLiveData = MutableLiveData<DeviceResponse>()

    private suspend fun connectToDevice(context: Context, deviceMAC: String): Boolean {
        return suspendCoroutine {
            scope.launch {
                processController = it
                try {
                    activeDeviceIdentifier = deviceMAC
                    deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.CONNECTING, activeDeviceIdentifier))
                    //bleDevice = ArmBleDevice(context, deviceMAC)
                    // FixME
                    bleDevice = DummyBleDevice(context, deviceMAC)
                    if(bleDevice!!.connect(bleConnectionCallback)){
                        delay(20)
                        // FixME
                        if(bleDevice!!.requestHigherMtu(DummyBleDevice.MTU_SIZE)){
                            delay(500)
                            fetchDeviceEndpoint()
                            deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.CONNECTED, activeDeviceIdentifier))
                        } else {
                            bleDevice = null
                            deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.FAILED, activeDeviceIdentifier))
                            it.resume(true)
                        }
                    } else {
                        bleDevice = null
                        deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.FAILED, activeDeviceIdentifier))
                        it.resume(true)
                    }
                } catch (e: Throwable) {
                    LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                    deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.FAILED, activeDeviceIdentifier))
                }
            }
        }
    }

    fun disconnectFromDevice() {
        scope.launch {
            try {
                delay(20)
                bleDevice!!.disconnect()
                bleDevice = null
                deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.DISCONNECTED, activeDeviceIdentifier))
                if(processController != null){
                    processController?.resume(true)
                }
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.DISCONNECTED, activeDeviceIdentifier))
                if(processController != null){
                    processController?.resume(true)
                }
            }
        }
    }

    private suspend fun fetchDeviceEndpoint(): Boolean {
        return suspendCoroutine {
            scope.launch {
                var deviceEndpoint = bleDevice!!.readEndpoint()
                // FixME
                when (activeDeviceIdentifier) {
                    "FE:7E:7E:BD:AB:87" -> {
                        deviceEndpoint = "xFE:7E:7E:BD:AB:87"
                    }
                    "XD:7E:7E:BD:AB:70" -> {
                        deviceEndpoint = "026eead293eb926ca57ba92703c00000"
                    }
                    "DS:7E:7E:BD:AB:79" -> {
                        deviceEndpoint = "036eead293eb926ca57ba92703c00000"
                    }
                }
                activeDeviceIdentifier = deviceEndpoint
                responseLiveData.postValue(
                    DeviceResponse(DeviceResponseState.ENDPOINT, deviceEndpoint)
                )
                it.resume(true)
            }
        }
    }

    // FixME [ devices: List<BleDevice> ]
    fun connectDevices(context: Context,
                       devices: List<DumBleDevice>,
                       workflowID: String,
                       sdaToken: String,
                       commands: List<TaskDeviceCommand>){
        scope.launch {
            jobID = workflowID
            accessToken = sdaToken
            jobCommands = commands
            devices.forEach { device ->
                LogHelper.debug(TAG, "Connecting device-> Name: ${device.deviceName}, " +
                        "MAC: ${device.deviceAddress}, RSSI: ${device.deviceRSSI}")
                // Now connect to the device
                connectToDevice(context, device.deviceAddress)
                LogHelper.debug(TAG, "DeviceLock released, now moving to next device")
            }
            // Signal all-devices completed
            responseLiveData.postValue(DeviceResponse(
                DeviceResponseState.JOB_COMPLETED)
            )
        }
    }

    fun runJob(){
        scope.launch {
            LogHelper.debug(TAG, "runJob() All Done, now RUNNNN")
            // [ Showdown Time ]
            jobCommands?.forEach { taskDeviceCommand ->
                LogHelper.debug(TAG, "Sending command [ ${taskDeviceCommand.deviceCommand.command} ] to device")
                sendToDevice(taskDeviceCommand.taskID, taskDeviceCommand.deviceCommand)
                LogHelper.debug(TAG, "CommandLock released, now moving to next command")
            }
            // All commands are now completed
            LogHelper.debug(TAG, "All commands completed, exit")
            deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.COMPLETED, activeDeviceIdentifier))
        }
    }

    private suspend fun sendToDevice(taskID: String, deviceCommand: DeviceCommand): Boolean {
        var deviceResponse: DeviceResponse
        return suspendCoroutine {
            try {
                //LogHelper.debug(TAG, "sendMessageToDevice() deviceCommand: $deviceCommand")
                deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.RUNNING, activeDeviceIdentifier))
                val opResponse = SecuredDeviceAccess.sendMessage(accessToken,
                    deviceCommand.command, deviceCommand.commandParams, bleDevice)
                if(opResponse.blob.isNotEmpty()){
                    deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.COMMAND_COMPLETED, activeDeviceIdentifier))
                    deviceResponse = DeviceResponse(DeviceResponseState.SDA, activeDeviceIdentifier, opResponse, taskID)
                    responseLiveData.postValue(deviceResponse)
                } else {
                    deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.COMMAND_FAILED, activeDeviceIdentifier))
                    deviceResponse = DeviceResponse(DeviceResponseState.SDA, activeDeviceIdentifier, null, taskID)
                    responseLiveData.postValue(deviceResponse)
                }
                // Release the locks to avoid deadlock
                it.resume(true)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.COMMAND_FAILED, activeDeviceIdentifier))
                deviceResponse = DeviceResponse(DeviceResponseState.SDA, activeDeviceIdentifier, null, taskID)
                responseLiveData.postValue(deviceResponse)
                // Release the locks to avoid deadlock
                it.resume(true)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}