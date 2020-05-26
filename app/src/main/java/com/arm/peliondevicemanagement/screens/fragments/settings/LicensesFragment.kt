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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.components.adapters.LicenseAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.LicenseModel
import com.arm.peliondevicemanagement.constants.AppConstants.LICENSE_JSON_FILE
import com.arm.peliondevicemanagement.databinding.FragmentLicensesBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LicensesFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = LicensesFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentLicensesBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var licenseAdapter: LicenseAdapter? = null
    private var licenseList = arrayListOf<LicenseModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentLicensesBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        licenseList.clear()
        // Load from local-assets
        val fetchedLicenses = loadLicensesFromAssets()
        LogHelper.debug(TAG, "Fetched ${fetchedLicenses.size} license from assets")
        licenseList.addAll(fetchedLicenses)
        // Setup adapter and recycler-view
        licenseAdapter = LicenseAdapter(licenseList, this)
        viewBinder.rvLicenseList.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = licenseAdapter
        }
    }

    private fun loadLicensesFromAssets(): List<LicenseModel> {
        val fetchedJSON = PlatformUtils.getJsonFromAssets(
            this@LicensesFragment.requireContext(),
            LICENSE_JSON_FILE)

        val type = object: TypeToken<List<LicenseModel>>() {}.type
        return Gson().fromJson<ArrayList<LicenseModel>>(fetchedJSON, type)
    }

    override fun onItemClick(data: Any) {
        val license = data as LicenseModel
        navigateToLicenseViewFragment(license.license)
    }

    private fun navigateToLicenseViewFragment(licenseText: String) {
        Navigation.findNavController(viewBinder.root)
            .navigate(LicensesFragmentDirections
                .actionLicensesFragmentToLicenseViewFragment(licenseText))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
