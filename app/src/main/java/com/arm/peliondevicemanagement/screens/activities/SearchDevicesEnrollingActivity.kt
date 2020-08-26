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

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.state.devices.DevicesSearchState
import com.arm.peliondevicemanagement.databinding.ActivitySearchDevicesEnrollingBinding
import com.arm.peliondevicemanagement.helpers.LogHelper

class SearchDevicesEnrollingActivity : BaseActivity() {

    companion object {
        private val TAG: String = SearchDevicesEnrollingActivity::class.java.simpleName
    }

    private lateinit var viewBinder: ActivitySearchDevicesEnrollingBinding
    private lateinit var toolbar: Toolbar
    private lateinit var searchState: DevicesSearchState
    private val deviceFiltersArray = resources.getStringArray(R.array.devices_filters)

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme(false)
        super.onCreate(savedInstanceState)
        viewBinder = ActivitySearchDevicesEnrollingBinding.inflate(layoutInflater)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Arguments
        searchState = DevicesSearchState.valueOf(
            intent.getStringExtra(AppConstants.DEVICES_AND_ENROLLING_SEARCH)!!
        )
        // Initialize
        init()
    }

    private fun init() {
        LogHelper.debug(TAG, "->ActiveSearchState: ${searchState.name}")

        toolbar = viewBinder.toolbar
        when(searchState) {
            DevicesSearchState.DEVICES -> {
                setupToolbar(toolbar, getString(R.string.devices_text))
                // Add filters to the view
                setupDeviceFilters()
            }
            DevicesSearchState.ENROLLING_DEVICES -> {
                setupToolbar(toolbar, getString(R.string.enrolling_devices_text))
            }
        }
    }

    private fun setupDeviceFilters() {
        val filtersAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            deviceFiltersArray
        )
        viewBinder.filtersMenu.setText(deviceFiltersArray[2], false)
        viewBinder.filtersMenu.setAdapter(filtersAdapter)
        viewBinder.dropdownFiltersMenu.visibility = View.VISIBLE
    }

    private fun navigateBackToDeviceManagementActivity() {
        fireIntentWithFinish(Intent(this, DeviceManagementActivity::class.java), false)
    }

    override fun onSupportNavigateUp(): Boolean {
        navigateBackToDeviceManagementActivity()
        return super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToDeviceManagementActivity()
    }
}