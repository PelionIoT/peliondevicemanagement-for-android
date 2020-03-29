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
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceRunModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_PENDING
import com.arm.peliondevicemanagement.databinding.FragmentJobBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeDrawable
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeIntoTimeAgo
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeString

class JobFragment : Fragment() {

    companion object {
        private val TAG: String = JobFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobFragmentArgs by navArgs()

    private lateinit var workflowModel: WorkflowModel
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private var totalDevicesCompleted: Int = 0

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
    }

    private fun setupData() {
        if(workflowModel.workflowDevices.isNullOrEmpty()){
            workflowModel.workflowDevices = arrayListOf()
            workflowModel.workflowAUDs.forEach { aud_device ->
                workflowModel.workflowDevices
                    .add(WorkflowDeviceModel(
                        aud_device.substring(3, aud_device.length),
                        DEVICE_STATE_PENDING))
            }
            LogHelper.debug(TAG, "completedDevices: 0, " +
                    "pendingDevices: ${workflowModel.workflowDevices.size}")
        } else {
            totalDevicesCompleted = 0
            workflowModel.workflowDevices.forEach { device ->
                if(device.deviceState == DEVICE_STATE_COMPLETED){
                    totalDevicesCompleted++
                }
            }
            LogHelper.debug(TAG, "completedDevices: $totalDevicesCompleted, " +
                    "pendingDevices: ${workflowModel.workflowDevices.size - totalDevicesCompleted}")
        }

        workflowDeviceAdapter = WorkflowDeviceAdapter(workflowModel.workflowDevices)
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

        viewBinder.tvDeviceHeader.setOnClickListener {
            expandCollapseDevicesRecView()
        }

        val totalCompletedText = "$totalDevicesCompleted/${workflowModel.workflowDevices.size}"

        viewBinder.tvDeviceSubHeader.text = context!!
            .getString(R.string.devices_completed_format,
                totalCompletedText)

        viewBinder.rvDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowDeviceAdapter
        }

        viewBinder.runJobButton.setOnClickListener {
            navigateToJobRunFragment()
        }
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
        val jobRunBundle = WorkflowDeviceRunModel(
            workflowModel.workflowID,
            workflowModel.workflowName,
            workflowModel.workflowStatus,
            workflowModel.workflowTasks,
            workflowModel.workflowDevices,
            "Not available"
        )

        Navigation.findNavController(viewBinder.root)
            .navigate(JobFragmentDirections
                .actionJobFragmentToJobRunFragment(jobRunBundle))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
