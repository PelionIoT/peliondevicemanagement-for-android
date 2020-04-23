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

import android.Manifest
import android.R.attr.host
import android.R.attr.port
import android.app.Activity
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_SERVICE
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.pelionmobiletransportsdk.TransportManager
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


object PlatformUtils {

    private val TAG: String = PlatformUtils::class.java.simpleName
    const val REQUEST_PERMISSION = 9040

    fun isNetworkAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    suspend fun isInternetAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress("www.google.com", 80)
                    , 2000
                )
                return@withContext true
            }
        } catch (e: IOException) {
            // Either we have a timeout or unreachable host or failed DNS lookup
            return@withContext false
        }
    }

    fun buildErrorBottomSheetDialog(context: Activity,
                                    title: String,
                                    description: String,
                                    clickListener: View.OnClickListener): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.TransparentSheetDialog)
        // Inflate-view
        val sheetView = context.layoutInflater.inflate(R.layout.layout_error_dialog, null)
        val titleText = sheetView.findViewById<TextView>(R.id.tvTitle)
        val descriptionText = sheetView.findViewById<TextView>(R.id.tvDescription)
        titleText.text = title
        descriptionText.text = description
        // Set On-Click on the retry-button
        val retryButton = sheetView.findViewById<MaterialButton>(R.id.retryButton)
        retryButton.setOnClickListener(clickListener)
        bottomSheetDialog.setContentView(sheetView)
        return bottomSheetDialog
    }

    fun parseErrorResponseFromJson(json: String): ErrorResponse? {
        return if(json.startsWith("{") && json.endsWith("}")){
            val type = object : TypeToken<ErrorResponse>() {}.type
            Gson().fromJson(json, type)
        } else {
            null
        }
    }

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

    fun fetchAttributeColor(context: Context, attrID: Int): ColorDrawable {
        val attr = context.obtainStyledAttributes(intArrayOf(attrID))
        val attrResId = attr.getResourceId(0,0)
        val drawable = ColorDrawable(context.resources.getColor(attrResId))
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

    fun parseDateTimeString(inputString: Date, format: String = "dd-MM-yyyy"): String {
        val outputFormat = SimpleDateFormat(format, Locale.ENGLISH)
        return outputFormat.format(inputString)
    }

    fun parseJSONTimeIntoTimeAgo(inputString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val date = inputFormat.parse(inputString)
        return TimeAgo.getTimeAgo(date!!.time)
    }

    fun isBluetoothEnabled(): Boolean = TransportManager.isBluetoothEnabled()

    fun enableBluetooth(context: Context) = TransportManager.enableBluetooth(context)

    fun getBleInstance(): BleManager {
        val context = AppController.appController!!.applicationContext
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
            .addFilterServiceUuid(SDA_SERVICE)

        return bleBuilder.build()
    }

    fun isSDKEqualORHigher(version: Int): Boolean {
        return Build.VERSION.SDK_INT >= version
    }

    fun hasLocationPermission(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    fun requestLocationPermission(context: Activity){
        ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)
    }

    fun checkForLocationPermission(context: Activity): Boolean {
        // Check for the platform, if M or higher
        return if(isSDKEqualORHigher(Build.VERSION_CODES.M)){
            // Check if we already have it or not
            if(!hasLocationPermission(context)){
                // No? then make a new request
                requestLocationPermission(context)
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    fun isLocationServiceEnabled(context: Context): Boolean = TransportManager.isLocationServicesEnabled(context)

    fun openLocationServiceSettings(context: Context) = TransportManager.openLocationServicesSettings(context)

    fun openAppSettings(context: Context){
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null))
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settingsIntent)
    }

}