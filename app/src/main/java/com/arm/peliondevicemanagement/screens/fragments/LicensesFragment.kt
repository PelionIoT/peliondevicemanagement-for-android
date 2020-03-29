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

import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentLicensesBinding

/**
 * A simple [Fragment] subclass.
 */
class LicensesFragment : Fragment() {

    companion object {
        private val TAG: String = LicensesFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentLicensesBinding? = null
    private val viewBinder get() = _viewBinder!!

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
        viewBinder.notFoundView.errorText.text = "Under construction"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
