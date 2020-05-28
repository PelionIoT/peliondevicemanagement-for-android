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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid

import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.arm.pelionmobiletransportsdk.ble.callbacks.*

import java.util.ArrayList

abstract class BleManager
/**
 * Constructor.
 */
protected constructor(builder: Builder) {
    /**
     * Return status scan.
     *
     * @return mScanning
     */
    var isScanStarted: Boolean = false
        protected set
    protected var scanPeriod: Int = 0
    protected var scanFilters: List<ScanFilter>? = null
    protected var scanSettings: ScanSettings? = null

    init {
        this.scanPeriod = builder.scanPeriod
        this.scanFilters = builder.scanFilters
        this.scanSettings = builder.scanSettings
        initResources()
    }

    /**
     * Init resources.
     */
    private fun initResources() {
        this.isScanStarted = false
    }

    /**
     * Start scan.
     *
     * @param callback [BleScannerCallback] Callback used to deliver scan results.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    abstract fun startScan(callback: BleScannerCallback)

    /**
     * Stop scan.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN])
    abstract fun stopScan()

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    abstract fun connectGatt(context: Context, address: String, callback: BleGattConnectCallback) : Boolean

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN])
    abstract fun disconnectGatt()

    abstract fun closeGatt()

    abstract fun isGattConnected(): Boolean
    abstract fun getMaxMtuSupported(): Int

    abstract fun read(serviceUUID: String, characteristicUUID: String, callback: BleGattReadCallback)
    abstract fun write(serviceUUID: String, characteristicUUID: String, data: ByteArray, callback: BleGattWriteCallback)
    abstract fun startNotify(serviceUUID: String, characteristicUUID: String): Boolean
    abstract fun stopNotify(serviceUUID: String, characteristicUUID: String): Boolean
    abstract fun notifyWithCallback(serviceUUID: String, characteristicUUID: String, enabled: Boolean, callback: BleGattNotifyCallback)
    abstract fun requestHigherMtu(size: Int, callback: BleMtuChangedCallback)

    /**
     * Reset settings to default.
     */
    fun resetSettings() {
        this.scanPeriod = 2000 // 2s
        this.scanFilters = null
        this.scanSettings = null
    }

    class Builder {
        var scanPeriod: Int = 0
        val scanFilters: MutableList<ScanFilter>
        var scanSettings: ScanSettings? = null

        init {
            this.scanPeriod = 10000 // 10 seconds
            this.scanFilters = ArrayList()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.scanSettings = ScanSettings.Builder().build()
            }
        }

        /**
         * Add period of scan.
         * Period in Milliseconds.
         *
         * @param period [Integer]
         * @return [Builder]
         */
        fun addScanPeriod(period: Int): Builder {
            scanPeriod = period
            return this
        }

        /**
         * Add filter adress.
         *
         * @param deviceAddress [String]
         * @return [Builder]
         * @throws UnsupportedOperationException If version sdk < lollipop.
         */
        fun addFilterAddress(vararg deviceAddress: String): Builder {
            for (address in deviceAddress) {
                scanFilters.add(
                    ScanFilter.Builder()
                        .setDeviceAddress(address)
                        .build()
                )
            }
            return this
        }

        /**
         * Add filter service serviceUUID.
         *
         * @param serviceUuid [String]
         * @return [Builder]
         * @throws UnsupportedOperationException If version sdk < lollipop.
         */
        fun addFilterServiceUuid(vararg serviceUuid: String): Builder {
            for (uuid in serviceUuid) {
                scanFilters.add(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(uuid))
                        .build()
                )
            }
            return this
        }

        /**
         * Add filter device serviceName.
         *
         * @param deviceName [String]
         * @return [Builder]
         * @throws UnsupportedOperationException If version sdk < lollipop.
         */
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun addFilterName(vararg deviceName: String): Builder {
            for (name in deviceName) {
                scanFilters.add(
                    ScanFilter.Builder()
                        .setDeviceName(name)
                        .build()
                )
            }
            return this
        }

        /**
         * Add settings scan.
         *
         * @param scanSettings [ScanSettings]
         * @return [Builder]
         * @throws UnsupportedOperationException If version sdk < lollipop.
         */
        fun addSettingsScan(scanSettings: ScanSettings): Builder {
            this.scanSettings = scanSettings
            return this
        }

        /**
         * Build instance of BleManager
         *
         * @return [BleManager]
         * @throws UnsupportedOperationException If version sdk < lollipop.
         */
        fun build(): BleManager {
            return ScanManager(this)
        }
    }
}