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

package com.arm.peliondevicemanagement.transport.sda;

import com.arm.mbed.sda.proxysdk.ProxyException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SerialMessage {

    public static final byte[] BOM = new byte[] {0x6d, 0x62, 0x65, 0x64, 0x64, 0x62, 0x61, 0x70};

    public static byte[] formatSerialProtocolMessage(byte[] operationMsg) {

        byte[] digest = getDigest(operationMsg);
        byte[] msgSize = new byte[4];
        msgSize = toBytes(operationMsg.length);

        byte[] serialMsg = new byte[BOM.length + msgSize.length + operationMsg.length + digest.length];
        System.arraycopy(BOM, 0, serialMsg, 0, BOM.length);
        System.arraycopy(msgSize, 0, serialMsg, BOM.length, msgSize.length);
        System.arraycopy(operationMsg, 0, serialMsg, BOM.length + msgSize.length, operationMsg.length);
        System.arraycopy(digest, 0, serialMsg, BOM.length + msgSize.length + operationMsg.length, digest.length);
        return serialMsg;
    }

    public static byte[] getDigest(byte[] msg) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException ex) {
            throw new ProxyException("Failed to get SHA256 message digest factory");
        }
        md.update(msg);
        return md.digest();
    }

    private static byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

}
