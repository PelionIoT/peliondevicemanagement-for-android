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

package com.arm.peliondevicemanagement.screens.fragments.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.LoginHistoryAdapter
import com.arm.peliondevicemanagement.components.models.user.UserLoginHistory
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.databinding.FragmentActivityInfoBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A simple [Fragment] subclass.
 */
class ActivityInfoFragment : Fragment() {

    companion object {
        private val TAG: String = ActivityInfoFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentActivityInfoBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var loginHistoryAdapter: LoginHistoryAdapter? = null
    private var loginHistoryModelsList = arrayListOf<UserLoginHistory>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentActivityInfoBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        init()
    }

    private fun setupData() {
        if(!SharedPrefHelper.getStoredUserProfile().isNullOrBlank()) {
            val profileJson = SharedPrefHelper.getStoredUserProfile()
            val type = object: TypeToken<UserProfile>() {}.type
            val profile = Gson().fromJson<UserProfile>(profileJson, type)
            LogHelper.debug(TAG, "onStoredProfile(): $profile")
            loginHistoryModelsList = ArrayList(profile.loginHistory)
        } else {
            viewBinder.notFoundView.errorText.text = requireActivity().getString(R.string.no_login_history)
            viewBinder.notFoundView.root.visibility = View.VISIBLE
        }
    }

    private fun init() {
        loginHistoryAdapter = LoginHistoryAdapter(loginHistoryModelsList)
        viewBinder.rvLoginHistory.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = loginHistoryAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
