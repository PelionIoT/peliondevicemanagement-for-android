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

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewmodels.SDAViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_VERIFY
import com.arm.peliondevicemanagement.databinding.FragmentJobRunBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils.getBleInstance
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getDeviceCommands
import com.arm.pelionmobiletransportsdk.ble.BleDevice
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleScannerCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

@ExperimentalCoroutinesApi
class JobRunFragment : Fragment() {

    companion object {
        private val TAG: String = JobRunFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobRunBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobRunFragmentArgs by navArgs()

    private lateinit var jobRunModel: WorkflowModel
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private val jobRunTimeOut: Long = 60000
    private var totalDevicesCompleted: Int = 0
    private lateinit var jobRunTimer: CountDownTimer

    private var isScanCompleted: Boolean = false
    private var isJobCompleted: Boolean = false

    private lateinit var sdaViewModel: SDAViewModel

    private var bleManager: BleManager? = null
    private lateinit var mScannedDevices: ArrayList<BleDevice>

    private val bleScanCallback: BleScannerCallback = object: BleScannerCallback {
        override fun onBatchScanResults(results: List<ScanResult>) {
            // Do nothing
        }

        override fun onFinish() {
            LogHelper.debug(TAG, "BleScan->onFinish()")
            if(mScannedDevices.isNotEmpty()){
                isScanCompleted = true
                updateStopButtonText("Stop")
                connectDevices()
            } else {
                isScanCompleted = false
                updateDeviceStatusText("No device available")
                showHideProgressbar(false)
                updateStopButtonText("Retry")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            LogHelper.debug(TAG, "BleScan->onScanFailed() $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult, bleDevice: BleDevice) {
            val isDevicePresent = mScannedDevices.find { it.deviceAddress == bleDevice.deviceAddress }
            if(isDevicePresent == null){
                LogHelper.debug(TAG, "BleScan->onScanResult() Found_Device: $bleDevice")
                mScannedDevices.add(BleDevice(bleDevice.device, bleDevice.deviceRSSI))
            }
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback = object: OnBackPressedCallback(true){
        override fun handleOnBackPressed() {
            callOnBackPressed()
        }
    }

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
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        jobRunModel = args.workflowObject
        LogHelper.debug(TAG, "jobRunBundle: $jobRunModel")

        setupData()
        setupViews()
        setupListeners()
        setupScan()
    }

    private fun setupData() {
        workflowDeviceAdapter = WorkflowDeviceAdapter(jobRunModel.workflowDevices!!)

        totalDevicesCompleted = 0
        jobRunModel.workflowDevices?.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${jobRunModel.workflowDevices?.size?.minus(totalDevicesCompleted)}")

    }

    private fun setupViews() {
        viewBinder.tvName.text = jobRunModel.workflowName
        viewBinder.tvTasks.text = context!!.getString(
            R.string.total_tasks_format,
            jobRunModel.workflowTasks.size.toString())
        viewBinder.tvDeviceSubHeader.text = context!!.getString(
            R.string.devices_completed_format,
            "$totalDevicesCompleted/${jobRunModel.workflowDevices?.size}")

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
            if(isScanCompleted){
                showCautionDialog()
            } else {
                setupScan()
            }
        }
    }

    private fun setupListeners() {
        sdaViewModel = ViewModelProvider(this).get(SDAViewModel::class.java)
        sdaViewModel.deviceStateLiveData.observe(viewLifecycleOwner, Observer { stateResponse ->
            when(stateResponse) {
                DEVICE_STATE_CONNECTING -> {
                    LogHelper.debug(TAG, "Device_State: Connecting")
                }
                DEVICE_STATE_CONNECTED -> {
                    LogHelper.debug(TAG, "Device_State: Connected")
                    sdaViewModel.fetchDeviceEndpoint()
                }
                DEVICE_STATE_VERIFY -> {
                    LogHelper.debug(TAG, "Device_State: Verify")
                }
                DEVICE_STATE_RUNNING -> {
                    LogHelper.debug(TAG, "Device_State: Running")
                }
                DEVICE_STATE_COMPLETED -> {
                    LogHelper.debug(TAG, "Device_State: Completed")
                }
                DEVICE_STATE_DISCONNECTED -> {
                    LogHelper.debug(TAG, "Device_State: Disconnected")
                }
                DEVICE_STATE_FAILED -> {
                    LogHelper.debug(TAG, "Device_State: Failed")
                }
            }
        })

        sdaViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { deviceResponse ->
            LogHelper.debug(TAG, "DeviceResponse: $deviceResponse")
            if(deviceResponse.response.startsWith("sda")) {
                // SDA response
                if(deviceResponse.operationResponse != null){
                    // Success, now terminate connection
                    sdaViewModel.disconnectFromDevice()
                } else {
                    // Failed, now terminate connection
                    sdaViewModel.disconnectFromDevice()
                }
            } else {
                // Endpoint response
                if(deviceResponse.response.startsWith("endpoint")){
                    val endpoint = deviceResponse.response.substringAfter("endpoint:")
                    if(isEndpointMatch(endpoint)){
                        // Go forward and execute job
                        sdaViewModel.runJob()
                    } else {
                        LogHelper.debug(TAG, "Endpoint not matched: $endpoint")
                        // Terminate connection
                        //sdaViewModel.disconnectFromDevice()
                        sdaViewModel.runJob()
                    }
                } else {
                    // Terminate connection
                    sdaViewModel.disconnectFromDevice()
                }
            }
        })
    }

    private fun setupScan() {
        showHideProgressbar(true)
        updateDeviceStatusText("Scanning devices")
        mScannedDevices = arrayListOf()
        bleManager = getBleInstance()
        bleManager!!.startScan(bleScanCallback)
    }

    private fun updateDeviceStatusText(message: String) {
        viewBinder.tvDeviceSubHeader.text = resources.getString(R.string.status_format, message)
    }

    private fun updateStopButtonText(message: String) {
        viewBinder.stopButton.text = message
    }

    private fun showHideProgressbar(visibility: Boolean) = if(visibility) {
        viewBinder.progressBar.visibility = View.VISIBLE
    } else {
        viewBinder.progressBar.visibility = View.INVISIBLE
    }

    private fun isEndpointMatch(endpoint: String): Boolean {
        val isEndpointPresent = jobRunModel.workflowDevices?.find { it.deviceName == endpoint }
        return isEndpointPresent != null
    }

    private fun connectDevices() {
        updateDeviceStatusText("Found Devices")
        val deviceCommands = getDeviceCommands(jobRunModel.workflowTasks)
        LogHelper.debug(TAG, "DeviceCommand Size: ${deviceCommands.size}")
        sdaViewModel.connectDevices(requireContext(),
            mScannedDevices,
            jobRunModel.workflowID,
            jobRunModel.sdaToken!!.accessToken,
            deviceCommands)
    }

    private fun destroyObjects() {
        bleManager = null
        jobRunTimer.cancel()
        sdaViewModel.cancelAllRequests()
        _viewBinder = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyObjects()
    }

    private fun showCautionDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("Attention Required")
            .setMessage("You're about to cancel a running job,\nAre you sure?")
            .setPositiveButton("Yes, Stop") { _, _ ->
                Navigation.findNavController(viewBinder.root).navigateUp()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun callOnBackPressed() {
        if(!isJobCompleted){
            showCautionDialog()
        } else {
            Navigation.findNavController(viewBinder.root).navigateUp()
        }
    }

}
