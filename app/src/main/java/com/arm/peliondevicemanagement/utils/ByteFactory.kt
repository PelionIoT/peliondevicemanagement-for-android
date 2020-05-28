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

package com.arm.peliondevicemanagement.utils

import java.util.*
import kotlin.math.roundToInt

object ByteFactory {

    fun splitBytes(dataBuffer: ByteArray, splitLength: Int): Queue<ByteArray> {
        val byteQueue: Queue<ByteArray> = LinkedList()

        val pkgCount: Int = if(dataBuffer.size % splitLength == 0) {
            dataBuffer.size / splitLength
        } else {
            ((dataBuffer.size / splitLength + 1)).toDouble().roundToInt()
        }

        if(pkgCount > 0) {
            for (i in 0 until pkgCount){
                var dataPkg: ByteArray
                val j: Int

                if(pkgCount == 1 || i == pkgCount - 1) {
                    j = if (dataBuffer.size % splitLength == 0) splitLength else dataBuffer.size % splitLength
                    System.arraycopy(dataBuffer, i * splitLength, ByteArray(j).also { dataPkg = it }, 0, j)
                } else {
                    System.arraycopy(dataBuffer, i * splitLength, ByteArray(splitLength).also { dataPkg = it }, 0, splitLength)
                }
                byteQueue.offer(dataPkg)
            }
        }
        return byteQueue
    }

    fun joinBytes(dataQueue: Queue<ByteArray>): ByteArray {
        var byteArray: ByteArray = byteArrayOf()

        dataQueue.forEach { bytePacket->
            val tempBuffer = ByteArray(byteArray.size + bytePacket.size)
            System.arraycopy(byteArray, 0, tempBuffer, 0, byteArray.size)
            System.arraycopy(bytePacket, 0, tempBuffer, byteArray.size, bytePacket.size)
            byteArray = tempBuffer
        }
        return byteArray
    }
}
