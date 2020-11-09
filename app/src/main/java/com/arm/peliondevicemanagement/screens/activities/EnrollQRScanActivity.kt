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
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.viewmodels.EnrollingIoTDevicesViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.ACTIVITY_RESULT
import com.arm.peliondevicemanagement.constants.AppConstants.SCANNED_QR_CODE_EID
import com.arm.peliondevicemanagement.databinding.ActivityEnrollQRScanBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.json.JSONObject
import java.util.*

class EnrollQRScanActivity : BaseActivity(), ZXingScannerView.ResultHandler {

    private enum class PrimaryAction {
        RETRY_PERMISSION,
        RE_UPLOAD,
        FINISH
    }

    companion object {
        private val TAG: String = EnrollQRScanActivity::class.java.simpleName
    }

    private lateinit var viewBinder: ActivityEnrollQRScanBinding
    private lateinit var enrollingIoTDevicesViewModel: EnrollingIoTDevicesViewModel
    private var enrollmentIdentity: String? = null
    private var _primaryButtonActionState: PrimaryAction = PrimaryAction.RETRY_PERMISSION

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme(false)
        super.onCreate(savedInstanceState)
        viewBinder = ActivityEnrollQRScanBinding.inflate(layoutInflater)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
        setupListeners()
    }

    private fun init() {
        enrollingIoTDevicesViewModel = ViewModelProvider(this)
            .get(EnrollingIoTDevicesViewModel::class.java)

        checkCameraAndSetupQR()
    }

    private fun setupListeners() {
        enrollingIoTDevicesViewModel.getEnrollmentStatusLiveData()
            .observe(this, Observer { _ ->
                LogHelper.debug(TAG, "Enrollment successful of identity: $enrollmentIdentity")
                showUploadSuccessDialog()
            })

        enrollingIoTDevicesViewModel.getErrorResponseLiveData()
            .observe(this, Observer { error ->
                if(error != null) {
                    if(error.errorCode == 409 && error.errorType == "duplicate"){
                        // Enrollment identity is already claimed.
                        showAlreadyClaimedDialog()
                    } else {
                        showRetryUploadOrRetakeShotDialog()
                    }
                } else {
                    processTimeout()
                }
        })

        viewBinder.primaryButton.setOnClickListener {
            when(_primaryButtonActionState){
                PrimaryAction.RETRY_PERMISSION -> {
                    checkCameraAndSetupQR()
                }
                PrimaryAction.RE_UPLOAD -> {
                    uploadEnrollmentIdentity()
                }
                PrimaryAction.FINISH -> {
                    returnToEnrollmentScreen(true)
                }
            }
        }

        viewBinder.secondaryButton.setOnClickListener {
            setupQRScanView()
        }

        viewBinder.backButton.setOnClickListener {
            returnToEnrollmentScreen(false)
        }
    }

    private fun setPrimaryActionState(primaryAction: PrimaryAction) {
        _primaryButtonActionState = primaryAction
    }

    private fun processTimeout() {
        showSnackbar(viewBinder.root, "Time-out, try again.")
        showRetryUploadOrRetakeShotDialog()
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
        enrollmentIdentity = null
        showHideNoCameraView(false)
        LogHelper.debug(TAG, "Camera access granted, starting QR-scanner")
        viewBinder.scanQRView.setResultHandler(this)
        viewBinder.scanQRView.startCamera()
    }

    private fun showHideNoCameraView(visibility: Boolean) = if(visibility) {
        setPrimaryActionState(PrimaryAction.RETRY_PERMISSION)
        viewBinder.noCameraView.visibility = View.VISIBLE
        viewBinder.primaryButton.visibility = View.VISIBLE
        viewBinder.secondaryButton.visibility = View.GONE
        viewBinder.tvMessage.text = getString(R.string.camera_perm_desc)
    } else {
        viewBinder.noCameraView.visibility = View.GONE
        viewBinder.primaryButton.visibility = View.GONE
        viewBinder.secondaryButton.visibility = View.GONE
        viewBinder.tvMessage.text = getString(R.string.scan_qr_desc)
    }

    private fun showUploadInProgressDialog() {
        viewBinder.primaryButton.visibility = View.GONE
        viewBinder.secondaryButton.visibility = View.GONE
        viewBinder.progressBar.visibility = View.VISIBLE
        viewBinder.tvHeader.text = getString(R.string.scanned_qr_upload_text)
        viewBinder.tvMessage.text = getString(R.string.scanned_qr_upload_desc)
    }

    private fun showUploadSuccessDialog() {
        setPrimaryActionState(PrimaryAction.FINISH)
        viewBinder.secondaryButton.visibility = View.GONE
        viewBinder.progressBar.visibility = View.GONE
        viewBinder.primaryButton.visibility = View.VISIBLE
        viewBinder.primaryButton.icon = getDrawable(R.drawable.ic_check_light)
        viewBinder.primaryButton.text = getString(R.string.finish_text)
        viewBinder.tvHeader.text = getString(R.string.scanned_qr_uploaded_text)
        viewBinder.tvMessage.text = getString(R.string.scanned_qr_uploaded_desc)
    }

    private fun showAlreadyClaimedDialog() {
        setPrimaryActionState(PrimaryAction.FINISH)
        viewBinder.progressBar.visibility = View.GONE
        viewBinder.primaryButton.visibility = View.VISIBLE
        viewBinder.primaryButton.icon = getDrawable(R.drawable.ic_close_light)
        viewBinder.primaryButton.text = getString(R.string.close_text)
        viewBinder.secondaryButton.visibility = View.VISIBLE
        viewBinder.tvHeader.text = getString(R.string.scanned_qr_upload_already_exists_text)
        viewBinder.tvMessage.text = getString(R.string.scanned_qr_upload_already_exists_desc)
    }

    private fun showRetryUploadOrRetakeShotDialog() {
        setPrimaryActionState(PrimaryAction.RE_UPLOAD)
        viewBinder.progressBar.visibility = View.GONE
        viewBinder.primaryButton.visibility = View.VISIBLE
        viewBinder.primaryButton.text = getString(R.string.retry_text)
        viewBinder.secondaryButton.visibility = View.VISIBLE
        viewBinder.tvHeader.text = getString(R.string.re_upload_scanned_qr_text)
        viewBinder.tvMessage.text = getString(R.string.re_upload_scanned_qr_desc)
    }

    private fun uploadEnrollmentIdentity() {
        showUploadInProgressDialog()
        enrollingIoTDevicesViewModel.enrollDevice(enrollmentIdentity!!)
    }

    override fun handleResult(rawResult: Result?) {
        if(rawResult != null){
            LogHelper.debug(TAG, rawResult.text)
            LogHelper.debug(TAG, rawResult.barcodeFormat.toString())
            try {
                val scannedCode = JSONObject(rawResult.text)
                    .getString(SCANNED_QR_CODE_EID).toUpperCase(Locale.ENGLISH)
                enrollmentIdentity = scannedCode
                uploadEnrollmentIdentity()
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Invalid QR-Code, ${e.message}")
                showSnackbar(viewBinder.root, "Invalid QR-Code, try again")
                setupQRScanView()
            }
        }
    }

    private fun returnToEnrollmentScreen(isRefreshable: Boolean) {
        val returnIntent = Intent()
        returnIntent.putExtra(ACTIVITY_RESULT, isRefreshable)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
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
        returnToEnrollmentScreen(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        enrollmentIdentity = null
        enrollingIoTDevicesViewModel.cancelAllRequests()
        viewBinder.scanQRView.stopCamera()
    }
}