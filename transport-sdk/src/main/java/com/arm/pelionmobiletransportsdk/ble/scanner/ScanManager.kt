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

package com.arm.pelionmobiletransportsdk.ble.scanner

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log

import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.arm.pelionmobiletransportsdk.ble.BleDevice
import com.arm.pelionmobiletransportsdk.ble.callbacks.*
import com.arm.pelionmobiletransportsdk.ble.commons.GattAttributes
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ScanManager(builder: Builder) : BleManager(builder) {

    companion object {
        private val TAG = ScanManager::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
    }

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: BleScannerCallback? = null
    private var handler: Handler? = null
    private var runnable: Runnable? = null

    private var defaultMtuSize: Int = 20

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mGattConnectionState = STATE_DISCONNECTED

    private var mGattConnectCallback: BleGattConnectCallback? = null
    private var mGattReadCallback: BleGattReadCallback? = null
    private var mGattWriteCallback: BleGattWriteCallback? = null
    private var mGattNotifyCallback: BleGattNotifyCallback? = null
    private var mGattMtuChangedCallback: BleMtuChangedCallback? = null

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (mScanCallback == null || result == null) return
            mScanCallback!!.onScanResult(callbackType, result, BleDevice(result.device, result.rssi))
        }

        override fun onBatchScanResults(results: List<ScanResult>?) {
            super.onBatchScanResults(results)
            if (mScanCallback == null || results == null) return
            mScanCallback!!.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            if (mScanCallback == null) return
            mScanCallback!!.onScanFailed(errorCode)
        }
    }

    private val bleGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mGattConnectionState = STATE_CONNECTED
                Log.d(TAG, "Connected to GATT server.")
                mGattConnectCallback!!.onGattConnect()
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt!!.discoverServices())

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mGattConnectionState = STATE_DISCONNECTED
                Log.d(TAG, "Disconnected from GATT server.")
                mGattConnectCallback!!.onGattDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattConnectCallback!!.onServicesDiscovered(gatt, supportedGattServices)
            } else {
                Log.d(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(mGattReadCallback == null) return
                val buffer = characteristic.value
                val hexString = buffer.toHex()
                mGattReadCallback!!.onRead(hexString, characteristic)
            } else {
                Log.d(TAG, "onCharacteristicRead() FAILED")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(mGattWriteCallback == null && characteristic == null) return
                val buffer = characteristic!!.value
                val hexString = buffer.toHex()
                mGattWriteCallback!!.onWrite(hexString, buffer, characteristic)
            } else {
                Log.d(TAG, "onCharacteristicWrite() FAILED")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val data = characteristic.value.toHex()
            mGattConnectCallback!!.onCharacteristicChanged(data, characteristic.value, characteristic)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if(mGattNotifyCallback == null) return
            val isSuccessful = status == BluetoothGatt.GATT_SUCCESS
            if(isSuccessful)
                mGattNotifyCallback!!.onNotify(true)
            else
                mGattNotifyCallback!!.onNotify(false)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if(mtu > 20) defaultMtuSize = mtu
            if(mGattMtuChangedCallback == null) return
            mGattMtuChangedCallback!!.onMtuChanged(mtu, status)
        }
    }

    fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    init {
        mBluetoothAdapter = getBluetoothAdapter()
    }

    /**
     * Init Bluetooth Low Energy Scanner.
     */
    private fun getBluetoothAdapter() = BluetoothAdapter.getDefaultAdapter()

    override fun isGattConnected() : Boolean {
        return mGattConnectionState == STATE_CONNECTED
    }

    override fun getMaxMtuSupported(): Int {
        return defaultMtuSize
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    private fun getCharacteristicByUUID(serviceUUID: String, characteristicUUID: String): BluetoothGattCharacteristic? {
        var fetchedCharacteristic: BluetoothGattCharacteristic? = null
        val gattService = getServiceByUUID(serviceUUID)
        gattService?.characteristics?.forEach { gattCharacteristic ->
            if(characteristicUUID == gattCharacteristic.uuid.toString()){
                fetchedCharacteristic = gattCharacteristic
            }
        }
        return fetchedCharacteristic
    }

    private fun getServiceByUUID(serviceUUID: String): BluetoothGattService? {
        var fetchedService: BluetoothGattService? = null
        supportedGattServices!!.forEach { gattService ->
            if(serviceUUID == gattService.uuid.toString())
                fetchedService = gattService
        }
        return fetchedService
    }

    private fun getCharacteristicData(characteristic: BluetoothGattCharacteristic): String? {
        var data: String? = null
        val buffer = characteristic.value
        if(buffer != null && buffer.isNotEmpty()){
            val stringBuilder = StringBuilder(buffer.size)
            for (byteChar in buffer)
                stringBuilder.append(String.format("%02X ", byteChar))
            data = String(buffer) + "\n" + stringBuilder.toString()
        }
        return data
    }

    /**
     * Start scan.
     *
     * @param callback
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun startScan(callback: BleScannerCallback) {

        mScanCallback = callback
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        requireNotNull(mBluetoothLeScanner) { "Bluetooth LE not available" }

        mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, bleScanCallback)
        isScanStarted = true
        handler = Handler()

        runnable = Runnable { stopScan() }
        handler!!.postDelayed(runnable!!, scanPeriod.toLong())
    }

    /**
     * Stop scan.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN])
    override fun stopScan() {
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        if (mBluetoothLeScanner == null || mScanCallback == null) return
        mBluetoothLeScanner!!.stopScan(bleScanCallback)
        handler!!.removeCallbacks(runnable!!)
        mScanCallback!!.onFinish()
        mScanCallback = null
        mBluetoothLeScanner = null
        isScanStarted = false
    }

    override fun connectGatt(context: Context, address: String, callback: BleGattConnectCallback): Boolean {
        mGattConnectCallback = callback
        //mBluetoothAdapter = getBluetoothAdapter()
        requireNotNull(mBluetoothAdapter) { "BluetoothAdapter not initialized" }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mGattConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "BleDevice not found.  Unable to connectGatt.")
            return false
        }
        // We want to directly connectGatt to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, bleGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mGattConnectionState = STATE_CONNECTING
        return true
    }

    override fun disconnectGatt() {
        if(mBluetoothAdapter== null || mBluetoothGatt == null) return
        mBluetoothGatt!!.disconnect()
    }

    override fun closeGatt() {
        if(mBluetoothGatt == null) return
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
        mBluetoothDeviceAddress = null
        // Flush callbacks
        mGattNotifyCallback = null
        mGattWriteCallback = null
        mGattReadCallback = null
    }

    override fun read(serviceUUID: String, characteristicUUID: String, callback: BleGattReadCallback){
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return
        mGattReadCallback = callback
        val characteristic = getCharacteristicByUUID(serviceUUID, characteristicUUID)
        if(characteristic != null){
            mBluetoothGatt!!.readCharacteristic(characteristic)
        }
    }

    override fun write(serviceUUID: String, characteristicUUID: String, data: ByteArray, callback: BleGattWriteCallback){
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return
        mGattWriteCallback = callback
        val characteristic = getCharacteristicByUUID(serviceUUID, characteristicUUID)
        if(characteristic != null){
            characteristic.value = data
            mBluetoothGatt!!.writeCharacteristic(characteristic)
        }
    }

    override fun startNotify(serviceUUID: String, characteristicUUID: String): Boolean {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return false
        val characteristic = getCharacteristicByUUID(serviceUUID, characteristicUUID) ?: return false
        var success = mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)
        if(!success) return false
        val descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        success = mBluetoothGatt!!.writeDescriptor(descriptor)
        return success
    }

    override fun stopNotify(serviceUUID: String, characteristicUUID: String) : Boolean {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return false
        val characteristic = getCharacteristicByUUID(serviceUUID, characteristicUUID) ?: return false
        var success = mBluetoothGatt!!.setCharacteristicNotification(characteristic, false)
        if(!success) return false
        val descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
        descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        success = mBluetoothGatt!!.writeDescriptor(descriptor)
        return success
    }

    override fun notifyWithCallback(serviceUUID: String, characteristicUUID: String, enabled: Boolean, callback: BleGattNotifyCallback){
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return
        mGattNotifyCallback = callback
        val characteristic = getCharacteristicByUUID(serviceUUID, characteristicUUID)
        if(characteristic != null){
            var success = mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
            if(!success){
                mGattNotifyCallback!!.onNotify(false)
                return
            }
            val descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
            if(enabled)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            success = mBluetoothGatt!!.writeDescriptor(descriptor)
            if(!success){
                mGattNotifyCallback!!.onNotify(false)
                return
            }
        }
    }

    override fun requestHigherMtu(size: Int, callback: BleMtuChangedCallback) {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) return
        mGattMtuChangedCallback = callback
        Log.d(TAG, "Requesting for higher MTU.")
        mBluetoothGatt!!.requestMtu(size)
    }
}