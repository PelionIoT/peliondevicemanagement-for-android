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

package com.arm.peliondevicemanagement.screens.fragments.devices

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.EnrollingIoTDeviceAdapter
import com.arm.peliondevicemanagement.components.viewmodels.EnrollingIoTDevicesViewModel
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.constants.state.NetworkErrorState
import com.arm.peliondevicemanagement.databinding.FragmentEnrollingDevicesBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.screens.activities.DeviceManagementActivity
import com.arm.peliondevicemanagement.screens.fragments.jobs.JobFragment
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.android.material.bottomsheet.BottomSheetDialog

class EnrollingDevicesFragment : Fragment() {

    companion object {
        private val TAG: String = EnrollingDevicesFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentEnrollingDevicesBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var enrollingDevicesViewModel: EnrollingIoTDevicesViewModel
    private var enrollingDevicesAdapter = EnrollingIoTDeviceAdapter()

    private var errorBottomSheetDialog: BottomSheetDialog? = null
    private lateinit var retryButtonClickListener: View.OnClickListener

    private lateinit var activeActionState: NetworkErrorState

    private val refreshListener: SwipeRefreshLayout.OnRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        refreshContent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentEnrollingDevicesBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
    }

    private fun init() {
        setSwipeRefreshStatus(true)
        enrollingDevicesViewModel = ViewModelProvider(this).get(EnrollingIoTDevicesViewModel::class.java)
        enrollingDevicesViewModel.initEnrollingIoTDevicesLiveData()

        viewBinder.rvEnrollingDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = enrollingDevicesAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
    }

    private fun setupListeners() {
        viewBinder.swipeRefreshLayout.setOnRefreshListener(refreshListener)

        viewBinder.rvEnrollingDevices.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if(!enrollingDevicesAdapter.currentList.isNullOrEmpty()){
                    if(dy>0){
                        viewBinder.scanButton.hide()
                    } else {
                        viewBinder.scanButton.show()
                    }
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        viewBinder.scanButton.setOnClickListener {
            (activity as DeviceManagementActivity).showSnackbar(requireView(), "Under construction")
        }

        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
            when(activeActionState){
                NetworkErrorState.NO_NETWORK -> {
                    refreshContent()
                }
                NetworkErrorState.UNAUTHORIZED -> {
                    navigateToLogin()
                }
            }
        }

        enrollingDevicesViewModel.getEnrollingIoTDevices().observe(viewLifecycleOwner, Observer {
            if(it != null && it.isNotEmpty()){
                setSwipeRefreshStatus(false)
            }
            enrollingDevicesAdapter.submitList(it)
        })

        enrollingDevicesViewModel.getRefreshState().observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                LoadState.LOADING -> {
                    setSwipeRefreshStatus(true)
                }
                LoadState.LOADED -> {
                    // Do nothing
                }
                LoadState.DOWNLOADED -> {
                    setSwipeRefreshStatus(false)
                    showHideEnrollButton(true)
                }
                LoadState.FAILED -> {
                    setSwipeRefreshStatus(false)
                }
                LoadState.UNAUTHORIZED -> {
                    setSwipeRefreshStatus(false)
                    activeActionState = NetworkErrorState.UNAUTHORIZED
                    // Show unauthorized dialog
                    showErrorDialog(NetworkErrorState.UNAUTHORIZED)
                }
                LoadState.NO_NETWORK -> {
                    setSwipeRefreshStatus(false)
                    activeActionState = NetworkErrorState.NO_NETWORK
                    // Show no-network dialog
                    showErrorDialog(NetworkErrorState.NO_NETWORK)
                }
                else -> {
                    setSwipeRefreshStatus(false)
                    showHideEnrollButton(false)
                    showHide404View(true)
                }
            }
        })
    }

    private fun refreshContent() {
        LogHelper.debug(TAG, "refreshContent()")

        showHideEnrollButton(false)
        showHide404View(false)
        setSwipeRefreshStatus(true)

        enrollingDevicesAdapter.submitList(null)
        enrollingDevicesViewModel.refreshEnrollingDevices()
    }

    private fun setSwipeRefreshStatus(isRefreshing: Boolean) {
        viewBinder.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun showHide404View(visibility: Boolean) = if (visibility) {
        viewBinder.notFoundView.root.visibility = View.VISIBLE
    } else {
        viewBinder.notFoundView.root.visibility = View.GONE
    }

    private fun showHideEnrollButton(visibility: Boolean) = if(visibility){
        viewBinder.scanButton.visibility = View.VISIBLE
    } else {
        viewBinder.scanButton.visibility = View.GONE
    }

    private fun showErrorDialog(state: NetworkErrorState) {
        if(errorBottomSheetDialog != null) {
            // If previous dialog is already visible
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
        }

        when(state) {
            NetworkErrorState.NO_NETWORK -> {
                errorBottomSheetDialog = PlatformUtils.buildErrorBottomSheetDialog(
                    requireActivity(),
                    resources.getString(R.string.no_internet_text),
                    resources.getString(R.string.check_connection_text),
                    retryButtonClickListener
                )
            }
            NetworkErrorState.UNAUTHORIZED -> {
                errorBottomSheetDialog = PlatformUtils.buildErrorBottomSheetDialog(
                    requireActivity(),
                    resources.getString(R.string.unauthorized_text),
                    resources.getString(R.string.unauthorized_desc),
                    retryButtonClickListener,
                    resources.getString(R.string.re_login_text)
                )
            }
        }

        errorBottomSheetDialog!!.show()
    }

    private fun navigateToLogin() {
        (requireActivity() as DeviceManagementActivity).initiateTemporarySignOut()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        enrollingDevicesViewModel.cancelAllRequests()
        _viewBinder = null
    }
}