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

package com.arm.pelionmobiletransportsdk.ble.commons

import java.util.HashMap

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object GattAttributes {
    private val attributes = HashMap<String,String>()

    // Unknowns
    const val UNKNOWN_SERVICE = "Unknown Service"
    const val UNKNOWN_CHARACTERISTIC = "Unknown Characteristic"

    // Generic Services
    const val GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb"
    const val GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb"
    const val DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
    const val HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb"

    // Generic Characteristics
    const val DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
    const val SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
    const val APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb"
    const val MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"
    const val SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb"
    const val PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb"
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    const val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"

    init {
        // Generic Services.
        attributes[GENERIC_ACCESS] = "Generic Access"
        attributes[GENERIC_ATTRIBUTE] = "Generic Attribute"
        attributes[DEVICE_INFORMATION_SERVICE] = "Device Information Service"
        attributes[HEART_RATE] = "Heart Rate Service"

        // Generic Characteristics.
        attributes[DEVICE_NAME] = "Device Name"
        attributes[SERIAL_NUMBER] = "Serial Number"
        attributes[APPEARANCE] = "Appearance"
        attributes[MANUFACTURER_NAME] = "Manufacturer Name String"
        attributes[SERVICE_CHANGED] = "Service Changed"
        attributes[PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS] = "Peripheral Preferred Connection Parameters"
        attributes[HEART_RATE_MEASUREMENT] = "Heart Rate Measurement"
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }
}