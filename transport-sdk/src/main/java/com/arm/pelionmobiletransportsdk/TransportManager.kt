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

package com.arm.pelionmobiletransportsdk

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.arm.pelionmobiletransportsdk.exceptions.TransportManagerException

class TransportManager {

    companion object {

        const val DEFAULT_SCAN_PERIOD = 2000

        private fun isEmulator(): Boolean {
            return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")
        }

        private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

        private fun getBluetoothAdapter() = BluetoothAdapter.getDefaultAdapter()

        fun isBluetoothEnabled(): Boolean {
            if(isEmulator()) return false
            return getBluetoothAdapter().isEnabled
        }

        fun isBleSupported(packageManager: PackageManager): Boolean {
            if(isEmulator()) return false
            return !packageManager.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }

        fun enableBluetooth(context: Context) {
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        fun getBleBuilder(context: Context): BleManager.Builder {
            when {
                isEmulator() -> {
                    throw TransportManagerException("BLE not supported on emulator!")
                }
                !isBluetoothEnabled() -> {
                    throw TransportManagerException("Bluetooth not enabled!")
                }
                !isBleSupported(context.packageManager) -> {
                    throw TransportManagerException("BLE not supported on this device!")
                }
                else -> {
                    return BleManager.Builder()
                }
            }
        }

        fun isLocationServicesEnabled(context: Context): Boolean {
            // Check for location services
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return LocationManagerCompat.isLocationEnabled(locationManager)
        }

        fun openLocationServicesSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
}