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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceRunModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEFAULT_TIME_FORMAT
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.databinding.FragmentJobBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeDrawable
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getPermissionScopeFromTasks
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isValidSDAToken
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeIntoTimeAgo
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeString
import kotlinx.android.synthetic.main.fragment_job.*

class JobFragment : Fragment() {

    companion object {
        private val TAG: String = JobFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobFragmentArgs by navArgs()

    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var workflowModel: WorkflowModel
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private var totalDevicesCompleted: Int = 0
    private var isSDATokenValid: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentJobBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        workflowModel = args.workflowObject
        LogHelper.debug(TAG, "selectedJob: $workflowModel")

        setupData()
        setupViews()
        setupListeners()
    }

    private fun setupData() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)
        workflowDeviceAdapter = WorkflowDeviceAdapter(workflowModel.workflowDevices!!)
        fetchCompletedDevicesCount()
    }

    private fun setupViews() {
        // Details Card
        viewBinder.tvName.text = workflowModel.workflowName
        viewBinder.tvDescription.text = workflowModel.workflowDescription
        viewBinder.tvStatus.text = context!!.getString(
            R.string.status_format, workflowModel.workflowStatus)
        viewBinder.tvLocation.text = context!!.getString(
            R.string.location_format, workflowModel.workflowLocation)
        val creationDateTime = parseJSONTimeString(workflowModel.workflowCreatedAt) +
                " - " + parseJSONTimeIntoTimeAgo(workflowModel.workflowCreatedAt)
        viewBinder.tvCreatedAt.text = context!!.getString(
            R.string.created_at_format, creationDateTime)

        val totalCompletedText = "$totalDevicesCompleted/${workflowModel.workflowDevices!!.size}"

        viewBinder.tvDeviceSubHeader.text = context!!
            .getString(R.string.devices_completed_format,
                totalCompletedText)

        viewBinder.rvDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowDeviceAdapter
        }

        verifySDAToken()
    }

    private fun setupListeners() {
        workflowViewModel.getRefreshedSDAToken().observe(viewLifecycleOwner, Observer { tokenResponse ->
            if(tokenResponse != null){
                workflowModel.sdaToken = tokenResponse
                workflowViewModel.updateLocalSDAToken(
                    workflowModel.workflowID, tokenResponse)
            } else {
                showSnackbar("Failed to refresh token")
            }
            showHideSDAProgressbar(false)
            verifySDAToken()
        })

        viewBinder.refreshTokenButton.setOnClickListener {
            refreshSDAToken()
        }

        viewBinder.tvDeviceHeader.setOnClickListener {
            expandCollapseDevicesRecView()
        }

        viewBinder.runJobButton.setOnClickListener {
            if(isSDATokenValid){
                navigateToJobRunFragment()
            } else {
                showSnackbar("Refreshing secure-access, hang-on")
                refreshSDAToken()
            }
        }
    }

    private fun showSnackbar(message: String) {
        (activity as HostActivity).showSnackbar(
            viewBinder.root,message)
    }

    private fun fetchCompletedDevicesCount() {
        totalDevicesCompleted = 0
        workflowModel.workflowDevices?.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${workflowModel.workflowDevices!!.size - totalDevicesCompleted}")
    }

    private fun refreshSDAToken() {
        showHideRefreshTokenButton(false)
        showHideSDAProgressbar(true)
        // Fetch permission-scope
        val permissionScope = getPermissionScopeFromTasks(workflowModel.workflowTasks)
        // Create audienceList for now.
        val audience = "ep:016eead293eb926ca57ba92703c00000"
        val audienceList = arrayListOf<String>()
        audienceList.add(audience)
        workflowViewModel.refreshSDAToken(permissionScope, audienceList)
    }

    private fun verifySDAToken() {
        if(workflowModel.sdaToken != null){
            val expiresIn = workflowModel.sdaToken!!.expiresIn
            val expiryDate = parseJSONTimeString(expiresIn)
            val expiryTime = parseJSONTimeString(expiresIn, DEFAULT_TIME_FORMAT)
            val expiryDateTime = "$expiryDate, $expiryTime"
            if(isValidSDAToken(expiresIn)){
                viewBinder.tvValidTill.text = context!!.getString(
                    R.string.active_format, expiryDateTime)
                viewBinder.secureIconView.setImageDrawable(
                    fetchAttributeDrawable(context!!, R.attr.iconShieldGreen))
                showHideRefreshTokenButton(false)
                isSDATokenValid = true
                updateRunJobButtonText(context!!.getString(R.string.run_job))
            } else {
                viewBinder.tvValidTill.text = context!!.getString(
                    R.string.expired_format, expiryDateTime)
                viewBinder.secureIconView.setImageDrawable(
                    fetchAttributeDrawable(context!!, R.attr.iconShieldRed))
                showHideRefreshTokenButton(true)
                isSDATokenValid = false
                updateRunJobButtonText(context!!.getString(R.string.refresh))
            }
        } else {
            viewBinder.tvValidTill.text = context!!.getString(R.string.na)
            viewBinder.secureIconView.setImageDrawable(
                fetchAttributeDrawable(context!!, R.attr.iconShieldYellow))
            showHideRefreshTokenButton(true)
            isSDATokenValid = false
            updateRunJobButtonText(context!!.getString(R.string.refresh))
        }
    }

    private fun updateRunJobButtonText(text: String) {
        viewBinder.runJobButton.text = text
    }

    private fun showHideRefreshTokenButton(visibility: Boolean) = if(visibility){
        refreshTokenButton.visibility = View.VISIBLE
    } else {
        refreshTokenButton.visibility = View.GONE
    }

    private fun showHideSDAProgressbar(visibility: Boolean) = if(visibility){
        viewBinder.sdaProgressbar.visibility = View.VISIBLE
    } else {
        viewBinder.sdaProgressbar.visibility = View.GONE
    }

    private fun expandCollapseDevicesRecView() {
        TransitionManager.beginDelayedTransition(viewBinder.cardJobDevicesItem)
        if(viewBinder.rvDevices.visibility == View.GONE){
            viewBinder.rvDevices.visibility = View.VISIBLE
            viewBinder.tvDeviceHeader.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                fetchAttributeDrawable(context!!, R.attr.iconArrowUp),
                null)
        } else {
            viewBinder.rvDevices.visibility = View.GONE
            viewBinder.tvDeviceHeader.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                fetchAttributeDrawable(context!!, R.attr.iconArrowDown),
                null)
        }
    }

    private fun navigateToJobRunFragment() {
        val jobRunBundle = workflowModel.workflowDevices?.let {
            WorkflowDeviceRunModel(
                workflowModel.workflowID,
                workflowModel.workflowName,
                workflowModel.workflowStatus,
                workflowModel.workflowTasks,
                it,
                "Not available"
            )
        }

        Navigation.findNavController(viewBinder.root)
            .navigate(JobFragmentDirections
                .actionJobFragmentToJobRunFragment(jobRunBundle!!))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        workflowViewModel.cancelAllRequests()
    }

}
