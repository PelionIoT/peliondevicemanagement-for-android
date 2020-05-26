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
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.components.adapters.HelpAndSupportAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.HelpAndSupportModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.databinding.FragmentHelpAndSupportBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A simple [Fragment] subclass.
 */
class HelpAndSupportFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = HelpAndSupportFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentHelpAndSupportBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var helpAndSupportAdapter: HelpAndSupportAdapter? = null
    private var helpAndSupportModelsList = arrayListOf<HelpAndSupportModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentHelpAndSupportBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        init()
    }

    private fun setupData(){
        val fetchedJSON = PlatformUtils.getJsonFromAssets(
            this@HelpAndSupportFragment.requireContext(),
            AppConstants.ABOUT_JSON_FILE)
        val type = object: TypeToken<List<HelpAndSupportModel>>() {}.type
        val supportDataList = Gson().fromJson<ArrayList<HelpAndSupportModel>>(fetchedJSON, type)
        LogHelper.debug(TAG, "helpAndSupportData: $supportDataList")
        helpAndSupportModelsList = supportDataList
    }

    private fun init() {
        helpAndSupportAdapter = HelpAndSupportAdapter(helpAndSupportModelsList, this)
        viewBinder.rvHelpAndSupport.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = helpAndSupportAdapter
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as HelpAndSupportModel
        LogHelper.debug(TAG, "title: ${model.title}, url: ${model.url}")

        if(model.url != "redirect_cuis_licenses")
            navigateToWebViewFragment(model.url)
        else
            navigateToLicensesFragment()
    }

    private fun navigateToWebViewFragment(url: String) {
        Navigation.findNavController(viewBinder.root)
            .navigate(
                HelpAndSupportFragmentDirections.actionHelpAndSupportFragmentToWebViewFragment(
                    url
                )
            )
    }

    private fun navigateToLicensesFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(HelpAndSupportFragmentDirections.actionHelpAndSupportFragmentToLicensesFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
