/*
 * Copyright 2020 ARM Ltd.
 *
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

package com.arm.peliondevicemanagement.screens.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.ActivityEnrollQRScanBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class EnrollQRScanActivity : BaseActivity(), ZXingScannerView.ResultHandler {

    private lateinit var viewBinder: ActivityEnrollQRScanBinding

    companion object {
        private val TAG: String = EnrollQRScanActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityEnrollQRScanBinding.inflate(layoutInflater)
        initTheme(false)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
    }

    private fun init() {
        checkCameraAndSetupQR()

        viewBinder.retryButton.setOnClickListener {
            checkCameraAndSetupQR()
        }
    }

    private fun checkCameraAndSetupQR() {
        if(PlatformUtils.checkForRuntimePermission(this, Manifest.permission.CAMERA)){
            setupQRScanView()
        } else {
            LogHelper.debug(TAG, "Camera access not granted, cannot start QR-scanner")
            showHideNoCameraView(true)
        }
    }

    private fun setupQRScanView() {
        showHideNoCameraView(false)
        LogHelper.debug(TAG, "Camera access granted, starting QR-scanner")
        viewBinder.scanQRView.setResultHandler(this)
        viewBinder.scanQRView.startCamera()
    }

    private fun showHideNoCameraView(visibility: Boolean) = if(visibility) {
        viewBinder.noCameraView.visibility = View.VISIBLE
        viewBinder.retryButton.visibility = View.VISIBLE
        viewBinder.tvMessage.text = getString(R.string.camera_perm_desc)
    } else {
        viewBinder.noCameraView.visibility = View.GONE
        viewBinder.retryButton.visibility = View.GONE
        viewBinder.tvMessage.text = getString(R.string.scan_qr_desc)
    }

    override fun handleResult(rawResult: Result?) {
        if(rawResult != null){
            LogHelper.debug(TAG, rawResult.text)
            LogHelper.debug(TAG, rawResult.barcodeFormat.toString())
            // TODO [ verification ]
        }
    }

    @RequiresApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PlatformUtils.REQUEST_PERMISSION -> if(grantResults.isNotEmpty()) {
                val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if(!cameraAccepted){
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.attention_text))
                            .setMessage(resources.getString(R.string.camera_perm_desc))
                            .setPositiveButton(resources.getString(R.string.grant_text)) { _, _ ->
                                PlatformUtils.requestPermission(this, Manifest.permission.CAMERA)
                            }
                            .setNegativeButton(resources.getString(R.string.deny_text)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.attention_text))
                            .setMessage(resources.getString(R.string.camera_perm_denied_desc))
                            .setPositiveButton(resources.getString(R.string.open_settings_text)) { _, _ ->
                                PlatformUtils.openAppSettings(this)
                            }
                            .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    }
                } else {
                    setupQRScanView()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinder.scanQRView.stopCamera()
    }
}