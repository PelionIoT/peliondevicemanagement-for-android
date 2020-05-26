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

package com.arm.peliondevicemanagement.screens.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_CHARACTERISTIC
import com.arm.peliondevicemanagement.constants.AppConstants.SDA_SERVICE
import com.arm.peliondevicemanagement.constants.AppConstants.SERVICE_UUID_REGEX
import com.arm.peliondevicemanagement.databinding.FragmentSettingsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.ViewHostActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

class SettingsFragment : Fragment() {

    companion object {
        private val TAG: String = SettingsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentSettingsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var initiatedDModeMs: Long = 0
    private var developerModeStepCounter: Int = 5
    private var easterEggActionPressed: Boolean = false

    private val checkedChangeListener: RadioGroup.OnCheckedChangeListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
        when (checkedId) {
            R.id.rbThemeDark -> {
                setThemeAndCallRecreate(true)
            }
            R.id.rbThemeLight -> {
                setThemeAndCallRecreate(false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
    }

    private fun init() {
        LogHelper.debug(TAG, "darkThemeEnabled: ${SharedPrefHelper.isDarkThemeEnabled()}")
        if(SharedPrefHelper.isDarkThemeEnabled()){
            viewBinder.rbThemeDark.isChecked = true
        }

        // If debug-build, enable feature-flag
        if(BuildConfig.DEBUG){
            val appVersion = (activity as ViewHostActivity).getAppVersion() + " - [ DEBUG ]"
            viewBinder.tvVersion.text = appVersion
            viewBinder.buildInfoCard.visibility = View.VISIBLE

            if(SharedPrefHelper.getDeveloperOptions().isDeveloperModeEnabled()) {
                viewBinder.developerModeCard.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        viewBinder.rgTheme.setOnCheckedChangeListener(checkedChangeListener)

        viewBinder.bleScanSetupButton.setOnClickListener {
            openScanSetupDialog()
        }

        viewBinder.userActivityButton.setOnClickListener {
            navigateToActivityInfoFragment()
        }

        viewBinder.helpSupportButton.setOnClickListener {
            navigateToHelpAndSupportFragment()
        }

        viewBinder.buildInfoCard.setOnClickListener {
            if(SharedPrefHelper.getDeveloperOptions().isDeveloperModeEnabled()){
                showToast(resources.getString(R.string.developer_mode_already_enabled_text))
            } else {
                if(easterEggActionPressed){
                    initiateDeveloperMode()
                }
            }
        }

        viewBinder.buildInfoCard.setOnLongClickListener {
            showToast("Easter egg activated")
            easterEggActionPressed = true
            return@setOnLongClickListener true
        }

        viewBinder.developerModeCard.setOnClickListener {
            navigateToDeveloperModeFragment()
        }
    }

    private fun setThemeAndCallRecreate(isDark: Boolean) {
        if(isDark){
            SharedPrefHelper.setDarkThemeStatus(true)
        } else
            SharedPrefHelper.setDarkThemeStatus(false)

        (activity as ViewHostActivity).recreate()
    }

    private fun initiateDeveloperMode() = if((System.currentTimeMillis() - initiatedDModeMs < 2000) && developerModeStepCounter < 1) {
        enableDeveloperMode()
    } else {
        developerModeStepCounter--
        initiatedDModeMs = System.currentTimeMillis()
        showToast(resources.getString(R.string.developer_mode_format, developerModeStepCounter.toString()))
    }

    private fun enableDeveloperMode() {
        showToast(resources.getString(R.string.developer_mode_enabled_text))
        LogHelper.debug(TAG, "->Developer-Mode: Enabled")
        SharedPrefHelper.getDeveloperOptions().setDeveloperModeStatus(true)
        viewBinder.developerModeCard.visibility = View.VISIBLE
    }

    private fun showToast(message: String){
        (activity as ViewHostActivity).showToast(message)
    }

    private fun openScanSetupDialog() {
        val scanSetupDialog = BottomSheetDialog(requireContext(),
            R.style.TransparentSheetDialog)
        // Inflate-view
        val sheetView = requireActivity()
            .layoutInflater.inflate(R.layout.layout_scan_setup_dialog, null)

        val serviceUUIDLayout = sheetView.findViewById<TextInputLayout>(R.id.txtServiceUUIDHeader)
        val serviceCharUUIDLayout = sheetView.findViewById<TextInputLayout>(R.id.txtServiceCharUUIDHeader)

        val serviceUUIDText = sheetView.findViewById<TextInputEditText>(R.id.txtServiceUUID)
        val serviceCharUUIDText = sheetView.findViewById<TextInputEditText>(R.id.txtServiceCharUUID)

        if(SharedPrefHelper.getSDAServiceUUID().isNotEmpty()
            && SharedPrefHelper.getSDAServiceCharacteristicUUID().isNotEmpty()){
            LogHelper.debug(TAG, "->scanSetup() Found custom serviceUUIDs")
            serviceUUIDText.setText(SharedPrefHelper.getSDAServiceUUID())
            serviceCharUUIDText.setText(SharedPrefHelper.getSDAServiceCharacteristicUUID())
        } else {
            LogHelper.debug(TAG, "->scanSetup() Found default serviceUUIDs")
            serviceUUIDText.setText(SDA_SERVICE)
            serviceCharUUIDText.setText(SDA_CHARACTERISTIC)
        }

        val restoreButton = sheetView.findViewById<MaterialButton>(R.id.restoreButton)
        val saveButton = sheetView.findViewById<MaterialButton>(R.id.saveButton)

        restoreButton.setOnClickListener {
            SharedPrefHelper.restoreSDAServiceUUIDsToDefaults()
            LogHelper.debug(TAG, "->scanSetup() Restored serviceUUIDs to defaults")
            scanSetupDialog.dismiss()
            showToast("Restore complete")
        }

        saveButton.setOnClickListener {
            val serviceUUID = serviceUUIDText.text.toString()
            val serviceCharUUID = serviceCharUUIDText.text.toString()

            if (!validateForm(serviceUUIDLayout,
                    serviceCharUUIDLayout,
                    serviceUUID,
                    serviceCharUUID)) {
                return@setOnClickListener
            }
            SharedPrefHelper.storeSDAServiceUUIDs(serviceUUID, serviceCharUUID)
            LogHelper.debug(TAG, "->scanSetup() Custom serviceUUIDs saved")
            scanSetupDialog.dismiss()
            showToast("Save complete")
        }

        scanSetupDialog.setContentView(sheetView)
        scanSetupDialog.show()
    }

    private fun validateForm(serviceUUIDLayout: TextInputLayout,
                             serviceCharUUIDLayout: TextInputLayout,
                             serviceUUIDText: String,
                             serviceCharUUIDText: String): Boolean {
        var valid = true

        serviceUUIDLayout.error = when {
            serviceUUIDText.isBlank() -> {
                valid = false
                "Required"
            }

            !isValidUUID(serviceUUIDText.trim()) -> {
                valid = false
                "Invalid UUID"
            }

            else -> null
        }

        serviceCharUUIDLayout.error = when {
            serviceCharUUIDText.isBlank() -> {
                valid = false
                "Required"
            }

            !isValidUUID(serviceCharUUIDText.trim()) -> {
                valid = false
                "Invalid UUID"
            }

            else -> null
        }

        return valid
    }

    private fun isValidUUID(uuid: String): Boolean {
        var valid = true

        if(uuid.length != 36){
            valid = false
            return valid
        }

        if(!Pattern.matches(SERVICE_UUID_REGEX, uuid)){
            valid = false
        }

        return valid
    }

    private fun navigateToActivityInfoFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToActivityInfoFragment())
    }

    private fun navigateToHelpAndSupportFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToHelpAndSupportFragment())
    }

    private fun navigateToDeveloperModeFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToDeveloperOptionsFragment())
    }
}
