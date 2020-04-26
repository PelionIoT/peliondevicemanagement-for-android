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

package com.arm.peliondevicemanagement.transport.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.arm.mbed.sda.proxysdk.devices.AbstractDevice
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_CHARACTERISTIC
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_SERVICE
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.transport.ISerialDataSink
import com.arm.peliondevicemanagement.transport.sda.SerialMessage
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattConnectCallback
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattWriteCallback
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleMtuChangedCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.arm.peliondevicemanagement.utils.ByteFactory
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattReadCallback
import com.arm.pelionmobiletransportsdk.ble.commons.GattAttributes
import com.arm.pelionmobiletransportsdk.ble.commons.HexUtils
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class ArmBleDevice(private val context: Context, private val deviceMAC: String):
    AbstractBleDevice(), ISerialDataSink {

    companion object{
        private val TAG = ArmBleDevice::class.java.simpleName
        const val MTU_SIZE: Int = 244
        // Should always be 4 bytes less than the MTU
        private const val TMSN_MTU_SIZE: Int = 230
        private const val TIME_OUT_MILLISECONDS: Long= 20000
    }

    private var mBleManager: BleManager? = null
    private var processController: Continuation<Boolean>? = null
    private var isInvokedByConnect: Boolean = false

    private lateinit var operationResponse: ByteArray
    private lateinit var dataOutQueue: Queue<ByteArray>
    private var totalPacketsToWrite: Int = 0
    private var currentPosition: Int = 0
    private var globalNumber: Int = 0
    private var isAckReceived: Boolean = false
    private var isFinalResponseReceived: Boolean = false

    private var bleConnectionCallback: BleConnectionCallback? = null

    private val bleGattCallback = object: BleGattConnectCallback {
        override fun onCharacteristicChanged(hexString: String?, byteBuffer: ByteArray, characteristic: BluetoothGattCharacteristic) {
            //LogHelper.debug(TAG, "onCharacteristicChanged() size: ${byteBuffer.size}, data: ${byteBuffer.contentToString()}")
            onNewData(byteBuffer)
        }

        override fun onGattConnect() {
            Log.d(TAG, "->onGattConnect()")
        }

        override fun onGattDisconnect() {
            Log.d(TAG, "->onGattDisconnect()")

            if(bleConnectionCallback != null){
                bleConnectionCallback!!.onDisconnect()
            }

            if(isInvokedByConnect){
                if (processController == null) return
                processController!!.resume(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, service: List<BluetoothGattService>?) {
            Log.d(TAG, "->onServicesDiscover()  servicesCount: ${service!!.size}")
            if (processController == null) return
            if(startNotify()) {
                Log.d(TAG, "->startNotify(): Notifications enabled.")
                isInvokedByConnect = false
                processController!!.resume(true)
            } else {
                Log.d(TAG, "->startNotify(): Notifications can't be enabled.")
                isInvokedByConnect = false
                processController!!.resume(false)
            }
        }
    }

    init {
        LogHelper.debug(TAG, "->init() getBleInstance")
        mBleManager = BleManager.Builder().build()
    }

    private fun isConnected(): Boolean {
        return mBleManager!!.isGattConnected()
    }

    override suspend fun connect(callback: BleConnectionCallback): Boolean {
        return suspendCoroutine {
            //mBleManager = BleManager.Builder().build()
            isInvokedByConnect = true
            processController = it
            bleConnectionCallback = callback
            mBleManager!!.connectGatt(context, deviceMAC, bleGattCallback)
        }
    }

    override suspend fun disconnect(): Boolean {
        return suspendCoroutine {
            if(stopNotify()){
                Log.d(TAG, "->stopNotify() Notifications disabled.")
            } else {
                Log.d(TAG, "->stopNotify() Notifications can't be disabled.")
            }
            mBleManager!!.closeGatt()
            processController = null
            totalPacketsToWrite = 0
            currentPosition = 0
            mBleManager = null
            Log.d(TAG, "->onDisconnect()")
            it.resume(true)
        }
    }

    override suspend fun releaseLocks() {
        isFinalResponseReceived = true
        disconnect()
    }

    private fun startNotify(): Boolean {
        if(!isConnected()) return false
        return mBleManager!!.startNotify(SDA_SERVICE, SDA_CHARACTERISTIC)
    }

    private fun stopNotify(): Boolean {
        if(!isConnected()) return false
        return mBleManager!!.stopNotify(SDA_SERVICE, SDA_CHARACTERISTIC)
    }

    override suspend fun requestHigherMtu(size: Int): Boolean {
        if(!isConnected()) return false
        return suspendCoroutine {
            mBleManager!!.requestHigherMtu(size, object: BleMtuChangedCallback {
                override fun onMtuChanged(mtuSize: Int, status: Int) {
                    Log.d(TAG, "->onMtuChanged() size: $mtuSize bytes")
                    it.resume(true)
                }
            })
        }
    }

    override suspend fun readEndpoint(): String {
        if(!isConnected()) return "null"
        return suspendCoroutine {
            mBleManager!!.read(
                GattAttributes.DEVICE_INFORMATION_SERVICE,
                GattAttributes.SERIAL_NUMBER_CHARACTERISTIC,
                object: BleGattReadCallback {
                    override fun onRead(hexString: String, characteristic: BluetoothGattCharacteristic) {
                        //Log.d(TAG, "->onReadEndpoint() $hexString")
                        val endpoint = HexUtils.convertHexToString(hexString)
                        Log.d(TAG, "->Endpoint() $endpoint")
                        it.resume(endpoint)
                    }
                })
        }
    }

    private suspend fun write(packet: ByteArray): Boolean {
        if(!isConnected()) return false
        return suspendCoroutine {
            mBleManager!!.write(SDA_SERVICE,
                SDA_CHARACTERISTIC,
                    packet,
                    object : BleGattWriteCallback {
                        override fun onWrite(hexString: String, buffer: ByteArray, characteristic: BluetoothGattCharacteristic) {
                            //Log.d(TAG, "->onAir() $hexString")
                            currentPosition = totalPacketsToWrite - dataOutQueue.size
                            //Log.d(TAG, "->onWrite() currentQueuePosition: $currentPosition, remainingItemsInQueue: ${dataOutQueue.size}")
                            isAckReceived = true
                            it.resume(true)
                        }
            })
        }
    }

    private suspend fun doWrite(protocolMessage: ByteArray) {
        // Enable feature-flag, if debug-build
        val transmissionMtuSize: Int = if(BuildConfig.DEBUG){
            if(SharedPrefHelper.getDeveloperOptions().isMaxMTUDisabled()){
                18
            } else {
                TMSN_MTU_SIZE
            }
        } else {
            TMSN_MTU_SIZE
        }

        Log.d(TAG, "->doWrite() MaxTransmissionMTU: $transmissionMtuSize bytes")
        /*Log.d(TAG, "->doWrite() protocolMessage" +
                "\nsize: ${protocolMessage.size}," +
                "\ndata: ${protocolMessage.contentToString()}")*/

        var packetBuffer: ByteArray = byteArrayOf()

        val messageQueue: Queue<ByteArray> = if(protocolMessage.size == 46){
            ByteFactory.splitBytes(protocolMessage, 46)
        } else {
            ByteFactory.splitBytes(protocolMessage, (transmissionMtuSize - 8))
        }
        Log.d(TAG, "->doWrite() ItemsInMessageQueue: ${messageQueue.size}")

        // Construct packets
        var packetNumber = 1
        val messageSize = messageQueue.size
        var hasMore = 1
        messageQueue.forEach { messageChunk ->
            if(packetNumber == messageSize){
                hasMore = 0
            }

            /*Log.d(TAG, "->doWrite() sN: $globalNumber, " +
                    "fN: $packetNumber, isMoreFragment: $hasMore")*/

            val newPacket = PacketFactory(
                globalNumber,
                PacketFactory.createControlFrame(packetNumber, hasMore),
                (messageChunk.size),
                protocolMessage.size, false,
                byteArrayOf(0, 0), messageChunk
            ).getPacket()

            packetBuffer += newPacket
            packetNumber++
        }
        globalNumber++

        Log.d(TAG, "->doWrite() PacketBufferSize: ${packetBuffer.size}")

        // Construct packet queue
        dataOutQueue = if(packetBuffer.size == 54) {
            ByteFactory.splitBytes(packetBuffer, 54)
        } else {
            ByteFactory.splitBytes(packetBuffer, transmissionMtuSize)
        }

        // Count total packets to write
        totalPacketsToWrite = dataOutQueue.size
        Log.d(TAG, "->doWrite() ItemsInPacketQueue: ${dataOutQueue.size}, TotalPacketsToWrite: $totalPacketsToWrite")
        isAckReceived = true

        while (dataOutQueue.size > 0){
            // Terminate if operation aborted
            if(isFinalResponseReceived){
                LogHelper.debug(TAG, "->doWrite() Aborting write-operation")
                break
            }
            if(isAckReceived){
                isAckReceived = false
                delay(200)
                Log.d(TAG, "->doWrite() Sending packet to device.")
                write(dataOutQueue.poll()!!)
            }
        }

        // Now wait for final response
        Log.d(TAG, "->doWrite() Write complete, now waiting for response.")

        withTimeoutOrNull(TIME_OUT_MILLISECONDS){
            while (!isFinalResponseReceived){
                delay(2000)
                // Wait here for full response
            }
        }
        isFinalResponseReceived = false
    }

    override fun sendMessage(operationMessage: ByteArray): ByteArray {
        operationResponse = byteArrayOf()

        /*Log.d(TAG, "->sendMessage() operationMessage" +
                "\nsize: ${operationMessage.size}," +
                "\ndata: ${operationMessage.contentToString()}")*/

        val serialProtocolMessage = SerialMessage.formatSerialProtocolMessage(operationMessage)
        Log.d(TAG, "->sendMessage() Starting write operation.")
        runBlocking {
            doWrite(serialProtocolMessage)
        }

        Log.d(TAG, "->sendMessage() Request time-out, now returning back.")

        return operationResponse
    }

    override fun onNewData(dataBuffer: ByteArray) {
        // Parse response
        val packetReceived = PacketFactory.new(dataBuffer)
        Log.d(TAG, "->onNewData() [ Response received ]" +
                "\npacketSize: ${packetReceived.getPacket().size}," +
                "\npacketContent: ${packetReceived.getPacket().contentToString()}" +
                "\npayloadSize: ${packetReceived.getDataPayload().size}," +
                "\npayloadContent: ${packetReceived.getDataPayload().contentToString()}")

        val hasMore = PacketFactory.hasMoreFragment(packetReceived.getPacket()[1])
        if(hasMore){
            // There's more to the response
            val payload = packetReceived.getDataPayload()
            val tempBuffer = ByteArray(operationResponse.size + payload.size)
            System.arraycopy(operationResponse, 0, tempBuffer, 0, operationResponse.size)
            System.arraycopy(payload, 0, tempBuffer, operationResponse.size, payload.size)
            // Assign new buffer to operationResponse
            operationResponse = tempBuffer
            LogHelper.debug(TAG, "Waiting for more response, hasMore: true")
        } else {
            if(operationResponse.isNotEmpty()){
                // There's more to the response
                val payload = packetReceived.getDataPayload()
                val tempBuffer = ByteArray(operationResponse.size + payload.size)
                System.arraycopy(operationResponse, 0, tempBuffer, 0, operationResponse.size)
                System.arraycopy(payload, 0, tempBuffer, operationResponse.size, payload.size)
                // Assign new buffer to operationResponse
                operationResponse = tempBuffer
                LogHelper.debug(TAG, "Final response received, hasMore: false")
                isFinalResponseReceived = true
            } else {
                operationResponse = packetReceived.getDataPayload()
                LogHelper.debug(TAG, "Final response received")
                isFinalResponseReceived = true
            }
        }
    }
}