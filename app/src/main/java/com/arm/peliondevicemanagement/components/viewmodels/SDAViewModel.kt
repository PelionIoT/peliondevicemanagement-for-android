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
import com.arm.peliondevicemanagement.components.models.GenericBleDevice
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceResponse
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceStateResponse
import com.arm.peliondevicemanagement.components.models.workflow.task.TaskDeviceCommand
import com.arm.peliondevicemanagement.constants.ExecutionMode
import com.arm.peliondevicemanagement.constants.state.DeviceResponseState
import com.arm.peliondevicemanagement.constants.state.DeviceState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.transport.ble.*
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

    private var bleDevice: AbstractBleDevice? = null
    private val bleConnectionCallback = object: BleConnectionCallback {
        override fun onDisconnect() {
            deviceStateLiveData.postValue(
                DeviceStateResponse(DeviceState.DISCONNECTED, activeDeviceIdentifier)
            )
            isActiveDeviceDisconnected = true
        }
    }

    private var activeDeviceIdentifier: String = "Unknown Device"
    private var isActiveDeviceDisconnected: Boolean = false

    private var jobID: String? = null
    private var accessToken: String? = null
    private var jobCommands: List<TaskDeviceCommand>? = null
    private var commandLock: Boolean = false
    private var executionMode: ExecutionMode = ExecutionMode.PHYSICAL

    val deviceStateLiveData = MutableLiveData<DeviceStateResponse>()
    val responseLiveData = MutableLiveData<DeviceResponse>()

    fun setExecutionMode(mode: ExecutionMode){
        LogHelper.debug(TAG, "->setExecutionMode() ${mode.name} DEVICE")
        executionMode = mode
    }

    fun setCommandLock(locked: Boolean) {
        if(locked){
            LogHelper.debug(TAG, "->setCommandLock() Lock-attached")
        } else {
            LogHelper.debug(TAG, "->setCommandLock() Lock-released")
        }
        commandLock = locked
    }

    private suspend fun connectToDevice(context: Context, deviceMAC: String): Boolean {
        return suspendCoroutine {
            scope.launch {
                processController = it
                try {
                    isActiveDeviceDisconnected = false
                    activeDeviceIdentifier = deviceMAC
                    deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.CONNECTING, activeDeviceIdentifier))
                    bleDevice = when(executionMode){
                        ExecutionMode.PHYSICAL -> {
                            // Real-device operation
                            ArmBleDevice(context, deviceMAC)
                        }
                        ExecutionMode.VIRTUAL -> {
                            // Virtual-device operation
                            DummyBleDevice(deviceMAC)
                        }
                    }
                    if(bleDevice!!.connect(bleConnectionCallback)){
                        delay(20)
                        if(bleDevice!!.requestHigherMtu(ArmBleDevice.MTU_SIZE)){
                            //delay(500)
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
            if(!isActiveDeviceDisconnected) {
                try {
                    delay(20)
                    bleDevice!!.disconnect()
                    bleDevice = null
                    if(processController != null){
                        processController?.resume(true)
                    }
                } catch (e: Throwable) {
                    LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                    if(processController != null){
                        processController?.resume(true)
                    }
                }
            }
        }
    }

    private suspend fun fetchDeviceEndpoint(): Boolean {
        return suspendCoroutine {
            scope.launch {
                val deviceEndpoint = bleDevice!!.readEndpoint()
                activeDeviceIdentifier = deviceEndpoint
                responseLiveData.postValue(
                    DeviceResponse(DeviceResponseState.ENDPOINT, deviceEndpoint)
                )
                it.resume(true)
            }
        }
    }

    fun connectDevices(context: Context,
                       devices: List<GenericBleDevice>,
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
                setCommandLock(locked = true)
                sendToDevice(taskDeviceCommand.taskID, taskDeviceCommand.deviceCommand)
            }
            // Move to final stage
            LogHelper.debug(TAG, "All commands completed, exit")
            deviceStateLiveData.postValue(DeviceStateResponse(DeviceState.COMPLETED, activeDeviceIdentifier))
        }
    }

    private suspend fun sendToDevice(taskID: String, deviceCommand: DeviceCommand): Boolean {
        var deviceResponse: DeviceResponse
        return suspendCoroutine {
            try {
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
                runBlocking {
                    // Wait here for the task-logs to complete
                    while (commandLock){
                        delay(500)
                        // Looping
                    }
                }
                // Release the suspension to avoid deadlock
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

    fun cancelAllRequests() {
        scope.cancel()
        coroutineContext.cancel()
    }

}