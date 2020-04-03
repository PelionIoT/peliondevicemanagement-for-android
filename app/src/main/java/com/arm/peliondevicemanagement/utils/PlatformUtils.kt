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

package com.arm.peliondevicemanagement.utils

import android.bluetooth.le.ScanSettings
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import com.arm.mbed.sda.proxysdk.SdkUtil
import com.arm.mbed.sda.proxysdk.http.CreateAccessTokenRequest
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.data.SDATokenResponse
import com.arm.pelionmobiletransportsdk.TransportManager
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import java.io.InputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object PlatformUtils {

    private val TAG: String = PlatformUtils::class.java.simpleName

    fun getJsonFromAssets(context: Context, fileName: String): String? {
        val jsonString: String
        jsonString = try {
            val `is`: InputStream = context.assets.open(fileName)
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
        return jsonString
    }

    fun fetchAttributeDrawable(context: Context, attrID: Int): Drawable {
        val attr = context.obtainStyledAttributes(intArrayOf(attrID))
        val attrResId = attr.getResourceId(0,0)
        val drawable = context.resources.getDrawable(attrResId)
        attr.recycle()
        return drawable
    }

    fun parseJSONTimeString(inputString: String, format: String = "MMM dd, yyyy"): String {
        // default should be: dd-MM-yyyy, but the use-case is different
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat(format, Locale.ENGLISH)
        val date = inputFormat.parse(inputString)
        return outputFormat.format(date!!)
    }

    fun parseJSONTimeIntoTimeAgo(inputString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val date = inputFormat.parse(inputString)
        return TimeAgo.getTimeAgo(date!!.time)
    }

    fun getBleInstance(context: Context, serviceUUID: String = ""): BleManager {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)

        // Only works when using M or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scanSettings.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            scanSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        }

        val bleBuilder = TransportManager.getBleBuilder(context)
            .addScanPeriod(TransportManager.DEFAULT_SCAN_PERIOD)
            .addSettingsScan(scanSettings.build())

        if(serviceUUID.isNotEmpty()){
            bleBuilder.addFilterServiceUuid(serviceUUID)
        }

        return bleBuilder.build()
    }

    private fun createSDATokenRequest(popPemKey: String, scope: String, audience: List<String>): String {
        val request = CreateAccessTokenRequest()
        request.grantType = "client_credentials"
        request.cnf = popPemKey
        request.scope = scope
        request.audience = audience

        //LogHelper.debug(TAG, "createSDATokenRequest() -> $request")
        return request.toString()
    }

    private fun validateSDAToken(accessToken: String, popPemKey: String) {
        SdkUtil.validateTokenSanity(accessToken, popPemKey)
    }

    suspend fun fetchSDAToken(
        cloudRepository: CloudRepository,
        scope: String,
        audienceList: List<String>): SDATokenResponse? {
        return try {
            val popPemPubKey = SdkUtil.getPopPemPubKey()
            val request = createSDATokenRequest(popPemPubKey, scope, audienceList)
            val tokenResponse = cloudRepository.getSDAToken(request)
            validateSDAToken(tokenResponse?.accessToken!!, popPemPubKey)
            tokenResponse
        } catch (e: Throwable){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            null
        }
    }

}