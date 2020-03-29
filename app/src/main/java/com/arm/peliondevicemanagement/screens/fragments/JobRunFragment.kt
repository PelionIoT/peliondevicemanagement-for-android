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
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceRunModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.databinding.FragmentJobRunBinding
import com.arm.peliondevicemanagement.helpers.LogHelper

class JobRunFragment : Fragment() {

    companion object {
        private val TAG: String = JobRunFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobRunBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobRunFragmentArgs by navArgs()

    private lateinit var jobRunModel: WorkflowDeviceRunModel
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private val jobRunTimeOut: Long = 60000
    private var totalDevicesCompleted: Int = 0
    private lateinit var jobRunTimer: CountDownTimer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentJobRunBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        jobRunModel = args.jobRunObject
        LogHelper.debug(TAG, "jobRunBundle: $jobRunModel")

        setupData()
        setupViews()
    }

    private fun setupData() {
        workflowDeviceAdapter = WorkflowDeviceAdapter(ArrayList(jobRunModel.jobDevices))

        totalDevicesCompleted = 0
        jobRunModel.jobDevices.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${jobRunModel.jobDevices.size - totalDevicesCompleted}")

    }

    private fun setupViews() {
        viewBinder.tvName.text = jobRunModel.jobName
        viewBinder.tvTasks.text = context!!.getString(
            R.string.total_tasks_format,
            jobRunModel.jobTasks.size.toString())
        viewBinder.tvDeviceSubHeader.text = context!!.getString(
            R.string.devices_completed_format,
            "$totalDevicesCompleted/${jobRunModel.jobDevices.size}")

        jobRunTimer = object: CountDownTimer(jobRunTimeOut, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = (millisUntilFinished / 1000)
                viewBinder.tvTime.text = context!!.getString(
                    R.string.elapsed_time_format,
                    "$timeLeft sec"
                )
            }

            override fun onFinish() {
                viewBinder.tvTime.text = context!!.getString(
                    R.string.elapsed_time_format,
                    "0 sec")
            }
        }
        jobRunTimer.start()

        viewBinder.rvDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowDeviceAdapter
        }

        viewBinder.stopButton.setOnClickListener {
            Navigation.findNavController(viewBinder.root).navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        jobRunTimer.cancel()
        _viewBinder = null
    }


}
