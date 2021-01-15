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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.DevicesSearchAdapter
import com.arm.peliondevicemanagement.components.adapters.EnrollingDevicesSearchAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.listeners.SearchResultsListener
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.components.models.devices.IoTDevice
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.SEARCH_BUNDLE
import com.arm.peliondevicemanagement.constants.state.devices.DevicesFilters
import com.arm.peliondevicemanagement.constants.state.devices.DevicesSearchState
import com.arm.peliondevicemanagement.databinding.ActivitySearchDevicesEnrollingBinding
import com.arm.peliondevicemanagement.helpers.LogHelper

class SearchDevicesEnrollingActivity : BaseActivity(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = SearchDevicesEnrollingActivity::class.java.simpleName
    }

    private lateinit var viewBinder: ActivitySearchDevicesEnrollingBinding
    private lateinit var toolbar: Toolbar
    private lateinit var searchState: DevicesSearchState
    private lateinit var deviceFiltersArray: Array<String>

    private lateinit var searchBundle: Bundle

    private lateinit var devicesList: ArrayList<IoTDevice>
    private lateinit var enrollingList: ArrayList<EnrollingIoTDevice>

    private lateinit var devicesSearchAdapter: DevicesSearchAdapter
    private lateinit var enrollingDevicesSearchAdapter: EnrollingDevicesSearchAdapter

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false

        override fun onQueryTextChange(newText: String?): Boolean {
            showHideNoResultsView(false)
            when(searchState) {
                DevicesSearchState.DEVICES -> {
                    devicesSearchAdapter.filter.filter(newText)
                }
                DevicesSearchState.ENROLLING_DEVICES -> {
                    enrollingDevicesSearchAdapter.filter.filter(newText)
                }
            }
            return false
        }
    }

    private val searchResultsListener = object : SearchResultsListener {
        override fun onNoResultsFound() {
            LogHelper.debug(TAG, "Search returned no results!")
            showHideNoResultsView(true)
        }
    }

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
        searchBundle = intent.extras!!

        // Initialize
        init()
    }

    private fun init() {
        LogHelper.debug(TAG, "->ActiveSearchState: ${searchState.name}")

        toolbar = viewBinder.toolbar
        viewBinder.rvDevicesSearch.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }

        when(searchState) {
            DevicesSearchState.DEVICES -> {
                setupToolbar(toolbar, getString(R.string.devices_text))
                // Add filters to the view
                setupDeviceFilters()
                devicesList = searchBundle
                    .getParcelableArrayList<IoTDevice>(SEARCH_BUNDLE) as ArrayList<IoTDevice>
                LogHelper.debug(TAG, "->devicesList() size: ${devicesList.size}")
                devicesSearchAdapter = DevicesSearchAdapter(devicesList, this, searchResultsListener)
                viewBinder.rvDevicesSearch.adapter = devicesSearchAdapter
            }
            DevicesSearchState.ENROLLING_DEVICES -> {
                setupToolbar(toolbar, getString(R.string.enrolling_devices_text))
                enrollingList = searchBundle
                    .getParcelableArrayList<EnrollingIoTDevice>(SEARCH_BUNDLE) as ArrayList<EnrollingIoTDevice>
                LogHelper.debug(TAG, "->enrollingDevicesList() size: ${enrollingList.size}")
                enrollingDevicesSearchAdapter = EnrollingDevicesSearchAdapter(enrollingList, searchResultsListener)
                viewBinder.rvDevicesSearch.adapter = enrollingDevicesSearchAdapter
            }
        }
        // Set query listener
        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)
        // Activate search-bar
        viewBinder.searchBar.searchTextBox.isIconified = false
    }

    private fun setupDeviceFilters() {
        deviceFiltersArray = resources.getStringArray(R.array.devices_filters)
        val filtersAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            deviceFiltersArray
        )
        viewBinder.filtersMenu.setText(deviceFiltersArray[2], false)
        viewBinder.filtersMenu.setAdapter(filtersAdapter)
        viewBinder.dropdownFiltersMenu.visibility = View.VISIBLE

        viewBinder.filtersMenu.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                LogHelper.debug(TAG, "SelectedItem: ${deviceFiltersArray[position]}")
                when (position) {
                    0 -> {
                        devicesSearchAdapter.setFilterType(DevicesFilters.DEVICE_ID)
                    }
                    1 -> {
                        devicesSearchAdapter.setFilterType(DevicesFilters.DEVICE_NAME)
                    }
                    else -> {
                        devicesSearchAdapter.setFilterType(DevicesFilters.ENDPOINT_NAME)
                    }
                }
            }
    }

    private fun showHideNoResultsView(visibility: Boolean) {
        if(visibility){
            viewBinder.rvDevicesSearch.visibility = View.GONE
            viewBinder.noSearchResultsView.root.visibility = View.VISIBLE
        } else {
            viewBinder.noSearchResultsView.root.visibility = View.GONE
            viewBinder.rvDevicesSearch.visibility = View.VISIBLE
        }
    }

    override fun onItemClick(data: Any) {
        val deviceItem = data as IoTDevice
        LogHelper.debug(TAG, "SelectedItem: $deviceItem")
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