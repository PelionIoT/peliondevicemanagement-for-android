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

package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentSettingsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity

class SettingsFragment : Fragment() {

    companion object {
        private val TAG: String = SettingsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentSettingsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var initiatedDModeMs: Long = 0
    private var developerModeStepCounter: Int = 5

    private val checkedChangeListener: RadioGroup.OnCheckedChangeListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
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
            viewBinder.tvVersion.text = (activity as HostActivity).getAppVersion()
            viewBinder.buildInfoCard.visibility = View.VISIBLE

            if(SharedPrefHelper.getDeveloperOptions().isDeveloperModeEnabled()) {
                viewBinder.developerModeCard.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        viewBinder.rgTheme.setOnCheckedChangeListener(checkedChangeListener)

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
                initiateDeveloperMode()
            }
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

        (activity as HostActivity).setAppTheme(true)
        (activity as HostActivity).recreate()
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
        (activity as HostActivity).showToast(message)
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
