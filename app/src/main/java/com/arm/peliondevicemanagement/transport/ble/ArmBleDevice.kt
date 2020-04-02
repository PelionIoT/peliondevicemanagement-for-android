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
import com.arm.peliondevicemanagement.transport.ISerialDataSink
import com.arm.peliondevicemanagement.transport.sda.SerialMessage
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattConnectCallback
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattWriteCallback
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleMtuChangedCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.arm.peliondevicemanagement.utils.ByteFactory
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleGattReadCallback
import com.arm.pelionmobiletransportsdk.ble.commons.GattAttributes
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class ArmBleDevice(private val context: Context, private val deviceMAC: String):
    AbstractDevice(), ISerialDataSink {

    companion object{
        private val TAG = ArmBleDevice::class.java.simpleName

        const val MTU_SIZE: Int = 244

        // Device Information Service
        private const val DEVICE_INFO_SERVICE: String = "0000180a-0000-1000-8000-00805f9b34fb"
        private const val SN_CHARACTERISTIC: String = ""

        // Service & Characteristic IDs for Secure-Device-Access
        private const val SDA_SERVICE: String = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        private const val SDA_CHARACTERISTIC: String = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
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

    private var bleConnectionCallback: ArmBleConnectionCallback? = null

    private val bleGattCallback = object: BleGattConnectCallback {
        override fun onCharacteristicChanged(hexString: String?, byteBuffer: ByteArray, characteristic: BluetoothGattCharacteristic) {
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
        mBleManager = BleManager.Builder().build()
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    fun isConnected(): Boolean {
        return mBleManager!!.isGattConnected()
    }

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine<Boolean> {
            isInvokedByConnect = true
            processController = it
            mBleManager!!.connectGatt(context, deviceMAC, bleGattCallback)
        }
    }

    suspend fun connect(callback: ArmBleConnectionCallback): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine<Boolean> {
            isInvokedByConnect = true
            processController = it
            bleConnectionCallback = callback
            mBleManager!!.connectGatt(context, deviceMAC, bleGattCallback)
        }
    }

    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine<Boolean> {
            if(stopNotify()){
                Log.d(TAG, "->stopNotify() Notifications disabled.")
            } else {
                Log.d(TAG, "->stopNotify() Notifications can't be disabled.")
            }
            mBleManager!!.closeGatt()
            processController = null
            totalPacketsToWrite = 0
            currentPosition = 0
            Log.d(TAG, "->onDisconnect()")
            it.resume(true)
        }
    }

    private fun startNotify(): Boolean {
        if(!isConnected()) return false
        return mBleManager!!.startNotify(SDA_SERVICE, SDA_CHARACTERISTIC)
    }

    private fun stopNotify(): Boolean {
        if(!isConnected()) return false
        return mBleManager!!.stopNotify(SDA_SERVICE, SDA_CHARACTERISTIC)
    }

    suspend fun requestHigherMtu(size: Int): Boolean = withContext(Dispatchers.IO) {
        if(!isConnected()) return@withContext false
        return@withContext suspendCoroutine<Boolean> {
            mBleManager!!.requestHigherMtu(size, object: BleMtuChangedCallback {
                override fun onMtuChanged(mtuSize: Int, status: Int) {
                    Log.d(TAG, "->onMtuChanged() size: $mtuSize bytes")
                    it.resume(true)
                }
            })
        }
    }

    suspend fun readEndpoint(): String = withContext(Dispatchers.IO) {
        if(!isConnected()) return@withContext "null"
        return@withContext suspendCoroutine<String> {
            mBleManager!!.read(DEVICE_INFO_SERVICE,
                SN_CHARACTERISTIC,
                object: BleGattReadCallback {
                    override fun onRead(data: String?, characteristic: BluetoothGattCharacteristic) {
                        Log.d(TAG, "->onReadEndpoint() $data")
                        it.resume(data!!)
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
                            Log.d(TAG, "->onAir() $hexString")
                            currentPosition = totalPacketsToWrite - dataOutQueue.size
                            Log.d(TAG, "->onWrite() currentQueuePosition: $currentPosition, remainingItemsInQueue: ${dataOutQueue.size}")
                            isAckReceived = true
                            it.resume(true)
                        }
            })
        }
    }

    private suspend fun doWrite(protocolMessage: ByteArray) = withContext(Dispatchers.IO) {
        val currentMtuSize = MTU_SIZE - 4

        Log.d(TAG, "->doWrite() MaxSupportedMTU: $currentMtuSize bytes")
        Log.d(TAG, "->doWrite() protocolMessage" +
                "\nsize: ${protocolMessage.size}," +
                "\ndata: ${protocolMessage.contentToString()}")

        var packetBuffer: ByteArray = byteArrayOf()

        val messageQueue: Queue<ByteArray> = if(protocolMessage.size == 46){
            ByteFactory.splitBytes(protocolMessage, 46)
        } else {
            // fixME [ do not touch this ]
            ByteFactory.splitBytes(protocolMessage, (currentMtuSize - 9))
            //ByteFactory.splitBytes(protocolMessage, (200 - 9))
        }
        Log.d(TAG, "->doWrite() ItemsInMessageQueue: ${messageQueue.size}")

        // Construct packets
        var packetNumber = 1
        val messageSize = messageQueue.size
        var isMore = 1
        messageQueue.forEach { messageChunk ->
            if(packetNumber == messageSize){
                isMore = 0
            }

            Log.d(TAG, "->doWrite() sN: $globalNumber, fN: $packetNumber, isMoreFragment: $isMore")

            val newPacket = PacketFactory(
                globalNumber,
                PacketFactory.createControlFrame(
                    packetNumber,
                    isMore
                ),
                (messageChunk.size),
                protocolMessage.size, false,
                byteArrayOf(0, 0), messageChunk,
                32
            ).getPacket()

            packetBuffer += newPacket
            packetNumber++
        }
        globalNumber++

        Log.d(TAG, "->doWrite() PacketBufferSize: ${packetBuffer.size}")

        // Construct packet queue
        dataOutQueue = if(packetBuffer.size == 55) {
            ByteFactory.splitBytes(packetBuffer, 55)
        } else {
            // fixME [ do not touch this ]
            ByteFactory.splitBytes(packetBuffer, currentMtuSize)
            //ByteFactory.splitBytes(packetBuffer, 200)
        }
        // Count total packets to write
        totalPacketsToWrite = dataOutQueue.size
        Log.d(TAG, "->doWrite() ItemsInPacketQueue: ${dataOutQueue.size}, TotalPacketsToWrite: $totalPacketsToWrite")

        isAckReceived = true

        while (dataOutQueue.size > 0){
            if(isAckReceived){
                isAckReceived = false
                delay(200)
                Log.d(TAG, "->doWrite() Sending packet to device.")
                write(dataOutQueue.poll()!!)
            }
        }

        delay(5000)
        // Now wait for final response
        Log.d(TAG, "->doWrite() Write complete, now waiting for response.")
    }

    override fun sendMessage(operationMessage: ByteArray): ByteArray {
        operationResponse = byteArrayOf()

        Log.d(TAG, "->sendMessage() operationMessage" +
                "\nsize: ${operationMessage.size}," +
                "\ndata: ${operationMessage.contentToString()}")

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

        operationResponse = packetReceived.getDataPayload()


        // Append new data with existing data.
        /*val tempBuffer = ByteArray(operationResponse.size + dataBuffer.size)
        System.arraycopy(operationResponse, 0, tempBuffer, 0, operationResponse.size)
        System.arraycopy(dataBuffer, 0, tempBuffer, operationResponse.size, dataBuffer.size)
        // Assign new buffer to operationResponse
        operationResponse = tempBuffer*/
    }

    interface ArmBleConnectionCallback {
        fun onDisconnect()
    }

}