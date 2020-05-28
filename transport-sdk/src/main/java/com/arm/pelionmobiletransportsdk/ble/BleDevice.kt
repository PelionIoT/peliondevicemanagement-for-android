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

package com.arm.pelionmobiletransportsdk.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.os.Parcel
import android.os.Parcelable
import com.arm.pelionmobiletransportsdk.ble.commons.GattAttributes
import com.arm.pelionmobiletransportsdk.ble.commons.GattCharacteristic
import com.arm.pelionmobiletransportsdk.ble.commons.GattService

data class BleDevice(private val bluetoothDevice: BluetoothDevice, private val bluetoothRSSI: Int): Parcelable {

    val device: BluetoothDevice = bluetoothDevice
    var deviceName: String = if(bluetoothDevice.name == null){
        "Unknown Device"
    } else {
        bluetoothDevice.name
    }
    val deviceAddress: String = bluetoothDevice.address
    var deviceRSSI: Int = bluetoothRSSI
    var gattServices = ArrayList<GattService>()

    constructor(parcel: Parcel) : this(
        parcel.readParcelable<BluetoothDevice>(BluetoothDevice::class.java.classLoader) as BluetoothDevice,
        parcel.readInt()
    ) {
        deviceName = parcel.readString()!!
        deviceRSSI = parcel.readInt()
    }

    fun setGattServices(gattServices: List<BluetoothGattService>?){
        if (gattServices == null) return
        this.gattServices.clear()
        val unknownServiceString = "Unknown Service"
        val unknownCharacteristicString = "Unknown Characteristic"

        for (gattService in gattServices){
            val serviceUUID = gattService.uuid.toString()
            val serviceName = GattAttributes.lookup(serviceUUID, unknownServiceString)
            val serviceCharacteristics = ArrayList<GattCharacteristic>()

            for(gattCharacteristic in gattService.characteristics){
                val characteristicUUID = gattCharacteristic.uuid.toString()
                val characteristicName = GattAttributes.lookup(characteristicUUID,
                    unknownCharacteristicString)

                serviceCharacteristics.add(
                    GattCharacteristic(characteristicName,
                    characteristicUUID, gattCharacteristic)
                )
            }

            this.gattServices.add(GattService(serviceName, serviceUUID,
                gattService, serviceCharacteristics))
        }
    }

    fun updateDevice(updatedDevice: BleDevice){
        this.deviceRSSI = updatedDevice.deviceRSSI
        this.gattServices = updatedDevice.gattServices
    }

    override fun toString(): String {
        return "BleDevice { Name: $deviceName, MAC: $deviceAddress, " +
                "ServicesCount: ${gattServices.size} RSSI: $deviceRSSI }"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bluetoothDevice, flags)
        parcel.writeInt(bluetoothRSSI)
        parcel.writeString(deviceName)
        parcel.writeInt(deviceRSSI)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BleDevice> {
        override fun createFromParcel(parcel: Parcel): BleDevice {
            return BleDevice(parcel)
        }

        override fun newArray(size: Int): Array<BleDevice?> {
            return arrayOfNulls(size)
        }
    }
}