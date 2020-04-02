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
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceRunModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowTaskModel
import com.arm.peliondevicemanagement.components.viewmodels.SDAViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_PENDING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_VERIFY
import com.arm.peliondevicemanagement.databinding.FragmentJobRunBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils.getBleInstance
import com.arm.pelionmobiletransportsdk.ble.BleDevice
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleScannerCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
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
    private var scanController: Continuation<Boolean>? = null

    private var signalForward: Boolean = false
    private var connectQueue: Queue<BleDevice>? = null

    private lateinit var sdaViewModel: SDAViewModel

    private var bleManager: BleManager? = null
    private lateinit var mScannedDevices: ArrayList<BleDevice>

    private val bleScanCallback: BleScannerCallback = object: BleScannerCallback {
        override fun onBatchScanResults(results: List<ScanResult>) {
            // Do nothing
        }

        override fun onFinish() {
            LogHelper.debug(TAG, "BleScan->onFinish()")
            if(scanController == null) return
            scanController!!.resume(true)
        }

        override fun onScanFailed(errorCode: Int) {
            LogHelper.debug(TAG, "BleScan->onScanFailed() $errorCode")
            if(scanController == null) return
            scanController!!.resume(false)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult, bleDevice: BleDevice) {
            val isDevicePresent = mScannedDevices.find { it.deviceAddress == bleDevice.deviceAddress }
            if(isDevicePresent == null){
                LogHelper.debug(TAG, "BleScan->onScanResult() Found_Device: $bleDevice")
                mScannedDevices.add(BleDevice(bleDevice.device, bleDevice.deviceRSSI))
            }
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
        jobRunModel = args.jobRunObject

        /*jobRunModel = WorkflowDeviceRunModel("1",
            "HelloTest",
            "PENDING",
            listOf(WorkflowTaskModel("1",
                "T1","bla", false,
                listOf(), listOf())),
                listOf(WorkflowDeviceModel("01nd32", DEVICE_STATE_PENDING)),
                "")*/

        LogHelper.debug(TAG, "jobRunBundle: $jobRunModel")

        setupData()
        setupViews()
        setupListeners()
        //setupScan()
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
                    signalForward = true
                }
                DEVICE_STATE_FAILED -> {
                    LogHelper.debug(TAG, "Device_State: Failed")
                }
            }
        })

        sdaViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { deviceResponse ->
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
                if(deviceResponse.response != "endpoint:null"){
                    val endpoint = deviceResponse.response.substringAfter("endpoint:")
                    if(isEndpointMatch(endpoint)){
                        // Go forward and execute job
                        runJob()
                    } else {
                        // Terminate connection
                        sdaViewModel.disconnectFromDevice()
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
        GlobalScope.launch {
            if(scanNearbyDevices() && mScannedDevices.isNotEmpty()){
                connectDevices()
            } else {
                updateDeviceStatusText("Scan Failed")
            }
        }

    }

    private fun updateDeviceStatusText(message: String) {
        viewBinder.tvDeviceSubHeader.text = resources.getString(R.string.status_format, message)
    }

    private fun showHideProgressbar(visibility: Boolean) = if(visibility) {
        viewBinder.progressBar.visibility = View.VISIBLE
    } else {
        viewBinder.progressBar.visibility = View.INVISIBLE
    }

    private suspend fun scanNearbyDevices(): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine<Boolean> {
            scanController = it
            mScannedDevices = arrayListOf()
            bleManager = getBleInstance(this@JobRunFragment.requireContext())
            bleManager!!.startScan(bleScanCallback)
        }
    }

    private fun isEndpointMatch(endpoint: String): Boolean {
        val isEndpointPresent = jobRunModel.jobDevices.find { it.deviceName == endpoint }
        return isEndpointPresent != null
    }

    private fun connectDevices() {
        updateDeviceStatusText("Found Devices")

        connectQueue = LinkedList()
        LogHelper.debug(TAG, "connectDevices() Adding devices to connectQueue")
        mScannedDevices.forEach { device->
            connectQueue!!.add(device)
        }

        while(connectQueue!!.size > 0){
            if(signalForward){
                signalForward = false
                val device = connectQueue!!.poll()

                LogHelper.debug(TAG, "connectDevice() Name: ${device!!.deviceName}, " +
                        "MAC: ${device.deviceAddress}")

                sdaViewModel.connectToDevice(
                    this@JobRunFragment.requireContext(),
                    device.deviceAddress)
            }
        }
    }

    private fun runJob() {
        LogHelper.debug(TAG, "Now running job on device.")
        signalForward = true
    }

    private fun destroyObjects() {
        signalForward = false
        scanController = null
        bleManager = null
        connectQueue = null
        jobRunTimer.cancel()
        sdaViewModel.cancelAllRequests()
        _viewBinder = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyObjects()
    }


}
