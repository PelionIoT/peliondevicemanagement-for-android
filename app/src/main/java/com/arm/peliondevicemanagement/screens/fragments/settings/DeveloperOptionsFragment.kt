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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.R

import com.arm.peliondevicemanagement.constants.ExecutionMode
import com.arm.peliondevicemanagement.databinding.FragmentDeveloperOptionsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.ViewHostActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeveloperOptionsFragment : Fragment() {

    companion object {
        private val TAG: String = DeveloperOptionsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentDeveloperOptionsBinding? = null
    private val viewBinder get() = _viewBinder!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _viewBinder = FragmentDeveloperOptionsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        setupView()
        setupListeners()
    }

    private fun setupView() {
        // Set default-cloud to the view
        when {
            SharedPrefHelper.getDeveloperOptions().getDefaultCloud() ==
                    getString(R.string.cloud_url_prod_text) -> {
                viewBinder.cloudProdCard.isChecked = true
            }
            SharedPrefHelper.getDeveloperOptions().getDefaultCloud() ==
                    getString(R.string.cloud_url_staging_text) -> {
                viewBinder.cloudStagingCard.isChecked = true
            }
            else -> {
                viewBinder.cloudIntegrationCard.isChecked = true
            }
        }

        if(SharedPrefHelper.getDeveloperOptions().isReAuthDisabled()){
            viewBinder.disableReAuthSwitch.isChecked = true
        }

        if(SharedPrefHelper.getDeveloperOptions().getSDAExecutionMode() == ExecutionMode.VIRTUAL.name){
            viewBinder.disableVirtualDeviceSwitch.isChecked = true
        }

        if(SharedPrefHelper.getDeveloperOptions().isMaxMTUDisabled()){
            viewBinder.disableMaxMTUSwitch.isChecked = true
        }

        if(SharedPrefHelper.getDeveloperOptions().isJobAutoSyncDisabled()){
            viewBinder.disableJobAutoSyncSwitch.isChecked = true
        }
        if(SharedPrefHelper.getDeveloperOptions().isAssetDownloadDisabled()){
            viewBinder.disableAssetDownloadSwitch.isChecked = true
        }
        if(SharedPrefHelper.getDeveloperOptions().isSDATokenDownloadDisabled()){
            viewBinder.disableSDATokenSwitch.isChecked = true
        }
    }

    private fun setupListeners() {
        viewBinder.cloudProdCard.setOnClickListener {
            signOutToChangeCloud(getString(R.string.cloud_url_prod_text))
        }

        viewBinder.cloudStagingCard.setOnClickListener {
            signOutToChangeCloud(getString(R.string.cloud_url_staging_text))
        }

        viewBinder.cloudIntegrationCard.setOnClickListener {
            signOutToChangeCloud(getString(R.string.cloud_url_integration_text))
        }

        viewBinder.disableReAuthSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "ReAuth Disabled: $isChecked")
            SharedPrefHelper.getDeveloperOptions().setReAuthDisabledStatus(isChecked)
        }

        viewBinder.disableVirtualDeviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "Virtual Device Enabled: $isChecked")
            if(isChecked){
                SharedPrefHelper.getDeveloperOptions().storeSDAExecutionMode(ExecutionMode.VIRTUAL.name)
            } else {
                SharedPrefHelper.getDeveloperOptions().storeSDAExecutionMode(ExecutionMode.PHYSICAL.name)
            }
        }

        viewBinder.disableMaxMTUSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "MaxMTU Disabled: $isChecked")
            SharedPrefHelper.getDeveloperOptions().setMaxMTUDisabledStatus(isChecked)
        }

        viewBinder.disableJobAutoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "Job Auto-Sync Disabled: $isChecked")
            SharedPrefHelper.getDeveloperOptions().setJobAutoSyncDisabledStatus(isChecked)
        }

        viewBinder.disableAssetDownloadSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "Asset Download Disabled: $isChecked")
            SharedPrefHelper.getDeveloperOptions().setAssetDownloadDisabledStatus(isChecked)
        }

        viewBinder.disableSDATokenSwitch.setOnCheckedChangeListener { _, isChecked ->
            LogHelper.debug(TAG, "SDA-Token Download Disabled: $isChecked")
            SharedPrefHelper.getDeveloperOptions().setSDATokenDownloadDisabledStatus(isChecked)
        }

        viewBinder.resetFeaturesCard.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(resources.getString(R.string.reset_developer_mode_text))
                .setMessage(resources.getString(R.string.reset_developer_mode_desc))
                .setPositiveButton(resources.getString(R.string.got_it_text)) { _, _ ->
                    LogHelper.debug(TAG, "Resetting developer-mode!")
                    SharedPrefHelper.getDeveloperOptions().resetOptions()
                    Navigation.findNavController(viewBinder.root).navigateUp()
                }
                .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    private fun signOutToChangeCloud(cloudURL: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(resources.getString(R.string.attention_text))
            .setMessage(resources.getString(R.string.cloud_flags_desc))
            .setPositiveButton(resources.getString(R.string.got_it_text)) { _, _ ->
                LogHelper.debug(TAG, "->signOutToChangeCloud()")
                LogHelper.debug(TAG, "Setting default-cloud to $cloudURL")
                // Save default-cloud to cache
                SharedPrefHelper.getDeveloperOptions().storeDefaultCloud(cloudURL)
                // Re-initialize cloudAPIService
                AppController.initCloudAPI()
                // Start sign-out
                SharedPrefHelper.clearEverything()
                (requireActivity() as ViewHostActivity).navigateToLogin()
            }
            .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

}
