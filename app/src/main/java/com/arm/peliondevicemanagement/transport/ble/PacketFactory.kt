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

package com.arm.peliondevicemanagement.transport.ble

class PacketFactory(packetNumber: Int, controlFrame: Byte,
                    packetLength: Int, totalPayloadSize: Int,
                    isReRequestedPacket: Boolean, packetReserved: ByteArray,
                    packetPayload: ByteArray) {

    // Packet Structure
    private var packetNumber: Byte =  0
    private var controlFrame: Byte = 0
    private var packetLength: Byte = 0
    private var totalPayloadSize: ByteArray = ByteArray(2)
    private var isReRequestedPacket: Byte = 0
    private var packetReserved: ByteArray = ByteArray(2)
    private var packetPayload: ByteArray = byteArrayOf()

    private var completeByteArray = ByteArray(8)

    init {
        this.packetNumber = packetNumber.toByte()
        this.controlFrame = controlFrame
        this.packetLength = packetLength.toByte()
        this.totalPayloadSize = bitShifter(totalPayloadSize)
        this.isReRequestedPacket = if(isReRequestedPacket) 1 else 0
        this.packetReserved[0] = packetReserved[0]
        this.packetReserved[1] = packetReserved[1]
        if(packetPayload.size > 232){
            throw ArrayIndexOutOfBoundsException("Packet Payload Max limit: 231")
        }
        this.packetPayload = packetPayload
        buildPacket()
    }

    companion object {
        fun new(packetArray: ByteArray): PacketFactory {
            return PacketFactory(
                packetArray[0].toInt(),
                packetArray[1],
                packetArray[2].toInt(),
                bitUnShifter(byteArrayOf(
                        packetArray[3],
                        packetArray[4])
                ),
                false, byteArrayOf(0, 0),
                packetArray.copyOfRange(8, packetArray.size - 1)
            )
        }

        private fun bitShifter(number: Int): ByteArray {
            // Extract the upper bit
            val upperBit = (number.and(0xFF00) shr 8)
            // Extract the lower bit
            val lowerBit = number.and(0x00FF)

            //print("Shifted Bits: $upperBit, $lowerBit\n")
            // Return both in form of a byteArray
            return byteArrayOf(lowerBit.toByte(), upperBit.toByte())
        }

        fun createControlFrame(fragmentNumber: Int, moreFragment: Int): Byte {
            val controlFrame = (fragmentNumber shl 3) or (moreFragment shl 2) or 1
            return controlFrame.toByte()
        }

        fun hasMoreFragment(controlFrame: Byte): Boolean {
            return (1 == (controlFrame.toInt() shr 2) and 1)
        }

        private fun bitUnShifter(byteArray: ByteArray): Int {
            val upperBit = byteArray[1].toInt()
            val lowerBit = byteArray[0].toInt()

            //print("Shifted Bits: $upperBit, $lowerBit\n")

            val reversedUpper = (upperBit.and(0xFF) shl 8)
            val reversedLower = (lowerBit.and(0xFF))

            /*print("Reversed Shifted Bits-> Upper: $reversedUpper, " +
                    "Lower: $reversedLower, Number: $reversedNumber\n")*/
            return reversedUpper + reversedLower
        }
    }

    private fun buildPacket() {
        // Construct packet header into main array
        completeByteArray[0] = packetNumber
        completeByteArray[1] = controlFrame
        completeByteArray[2] = packetLength
        completeByteArray[3] = totalPayloadSize[0]
        completeByteArray[4] = totalPayloadSize[1]
        completeByteArray[5] = isReRequestedPacket
        completeByteArray[6] = packetReserved[0]
        completeByteArray[7] = packetReserved[1]

        // Copy packet payload into main array
        val tempBuffer = ByteArray(completeByteArray.size + packetPayload.size)
        System.arraycopy(completeByteArray, 0, tempBuffer, 0, completeByteArray.size)
        System.arraycopy(packetPayload, 0, tempBuffer, completeByteArray.size, packetPayload.size)
        completeByteArray = tempBuffer
    }

    fun getPacket(): ByteArray {
        //print("PacketBuilder -> Packet: ${completeByteArray.contentToString()}\n\n")
        return completeByteArray
    }

    fun getDataPayload(): ByteArray {
        //print("PacketBuilder -> DataPayload: ${packetPayload.contentToString()}\n\n")
        return packetPayload
    }
}