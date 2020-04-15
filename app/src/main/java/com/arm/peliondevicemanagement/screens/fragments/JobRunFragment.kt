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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
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
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewmodels.SDAViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.ENDPOINT
import com.arm.peliondevicemanagement.constants.AppConstants.JOB_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.SDA
import com.arm.peliondevicemanagement.constants.DeviceState
import com.arm.peliondevicemanagement.databinding.FragmentJobRunBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.transport.ble.DumBleDevice
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.getBleInstance
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getDeviceCommands
import com.arm.pelionmobiletransportsdk.ble.BleDevice
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleScannerCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    private var totalDevicesCompleted: Int = 0

    private var isScanCompleted: Boolean = false
    private var isJobCompleted: Boolean = false

    private var activeItemPosition: Int = 0

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
                updateStopButtonText(
                    resources.getDrawable(R.drawable.ic_stop_light),
                    resources.getString(R.string.stop_text)
                )
                removeTemporaryDeviceItemFromList()
                connectDevices()
            } else {
                isScanCompleted = false
                updateDeviceItemInList(0,"No devices found", "Failed")
                updateStopButtonText(
                    resources.getDrawable(R.drawable.ic_refresh_light),
                    resources.getString(R.string.retry_text)
                )
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
        //setupScan()

        // FixME
        isScanCompleted = true
        connectDevices()
    }

    private fun setupData() {
        workflowDeviceAdapter = WorkflowDeviceAdapter(jobRunModel.workflowDevices!!)
        //jobRunModel.workflowDevices?.add(WorkflowDeviceModel("026eead293eb926ca57ba92703c00000", "Pending"))
    }

    private fun setTotalDevicesCompletedStatus(){
        totalDevicesCompleted = 0
        jobRunModel.workflowDevices?.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${jobRunModel.workflowDevices?.size?.minus(totalDevicesCompleted)}")

        val completedOutOfPending = "$totalDevicesCompleted/${jobRunModel.workflowDevices?.size}"
        viewBinder.tvCompleted.text = resources.getString(
            R.string.devices_completed_format,
            completedOutOfPending
        )
    }

    private fun setupViews() {
        viewBinder.tvName.text = jobRunModel.workflowName
        viewBinder.tvTasks.text = context!!.getString(
            R.string.total_tasks_format,
            jobRunModel.workflowTasks.size.toString())
        viewBinder.elapsedTimer.start()

        viewBinder.rvDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowDeviceAdapter
        }

        setTotalDevicesCompletedStatus()
    }

    private fun setupListeners() {

        viewBinder.rvDevices.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if(dy>0){
                    viewBinder.stopButton.hide()
                } else {
                    viewBinder.stopButton.show()
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        viewBinder.stopButton.setOnClickListener {
            if(isScanCompleted && !isJobCompleted){
                showCautionDialog()
            } else if(isJobCompleted) {
                callOnBackPressed()
            } else {
                setupScan()
            }
        }

        sdaViewModel = ViewModelProvider(this).get(SDAViewModel::class.java)
        sdaViewModel.deviceStateLiveData.observe(viewLifecycleOwner, Observer { stateResponse ->
            when(stateResponse.state) {
                DeviceState.CONNECTING -> {
                    LogHelper.debug(TAG, "DeviceState: Connecting, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    addTemporaryDeviceItemInList(stateResponse.deviceIdentifier, "Connecting")
                }
                DeviceState.CONNECTED -> {
                    LogHelper.debug(TAG, "DeviceState: Connected, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(0,stateResponse.deviceIdentifier, "Connected")
                    sdaViewModel.fetchDeviceEndpoint()
                }
                DeviceState.VERIFY -> {
                    LogHelper.debug(TAG, "DeviceState: Verify, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    removeTemporaryDeviceItemFromList()
                    activeItemPosition = getItemPosition(stateResponse.deviceIdentifier)
                    if(activeItemPosition >= 0){
                        updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Verify")
                    } else {
                        activeItemPosition = 0
                    }
                }
                DeviceState.RUNNING -> {
                    LogHelper.debug(TAG, "DeviceState: Running, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Running")
                }
                DeviceState.COMMAND_COMPLETED -> {
                    LogHelper.debug(TAG, "DeviceState: Command-Completed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Action Completed")
                }
                DeviceState.COMMAND_FAILED -> {
                    LogHelper.debug(TAG, "DeviceState: Command-Failed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Action Failed")
                }
                DeviceState.COMPLETED -> {
                    LogHelper.debug(TAG, "DeviceState: Completed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Completed")
                    sdaViewModel.disconnectFromDevice()
                }
                DeviceState.DISCONNECTED -> {
                    LogHelper.debug(TAG, "DeviceState: Disconnected, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    activeItemPosition = 0
                    setTotalDevicesCompletedStatus()
                }
                DeviceState.FAILED -> {
                    LogHelper.debug(TAG, "DeviceState: Failed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Failed")
                    sdaViewModel.disconnectFromDevice()
                }
            }
        })

        sdaViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { deviceResponse ->
            LogHelper.debug(TAG, "DeviceResponse: $deviceResponse")
            if(deviceResponse.response.startsWith(SDA)) {
                // SDA response
                if(deviceResponse.operationResponse != null){
                    // Success, now store run-logs and release command-lock
                    sdaViewModel.setDeviceCommandLockState(false)
                } else {
                    // Failed, now release command-lock
                    sdaViewModel.setDeviceCommandLockState(false)
                }
            } else {
                // Endpoint response
                if(deviceResponse.response.startsWith(ENDPOINT)){
                    val endpoint = deviceResponse.response.substringAfter("endpoint:")
                    if(isEndpointMatch(endpoint)){
                        // Go forward and execute job
                        LogHelper.debug(TAG, "Endpoint matched: $endpoint, ->runJob()")
                        sdaViewModel.runJob()
                    } else {
                        LogHelper.debug(TAG, "Endpoint not matched: $endpoint")
                        // Terminate connection
                        sdaViewModel.disconnectFromDevice()
                    }
                } else if(deviceResponse.response.startsWith(JOB_COMPLETED)) {
                    LogHelper.debug(TAG, "All devices are now completed")
                    finalizeEverything()
                }
            }
        })
    }

    private fun addTemporaryDeviceItemInList(deviceText: String, deviceState: String){
        jobRunModel.workflowDevices?.add(0, WorkflowDeviceModel(deviceText, deviceState))
        workflowDeviceAdapter.notifyItemChanged(0)
    }

    private fun updateDeviceItemInList(position: Int, deviceName: String, deviceState: String){
        jobRunModel.workflowDevices?.set(position, WorkflowDeviceModel(deviceName, deviceState))
        workflowDeviceAdapter.notifyItemChanged(position)
    }

    private fun removeTemporaryDeviceItemFromList() {
        jobRunModel.workflowDevices?.removeAt(0)
        workflowDeviceAdapter.notifyItemRemoved(0)
    }

    private fun getItemPosition(deviceIdentifier: String): Int {
        return jobRunModel.workflowDevices?.indexOf(
            WorkflowDeviceModel(deviceIdentifier, "Pending")
        )!!
    }

    private fun setupScan() {
        val scanDeviceItemPosition = getItemPosition("No devices found")
        if(scanDeviceItemPosition >= 0) {
            updateDeviceItemInList(0,"Scanning Devices", "Ongoing")
        } else {
            addTemporaryDeviceItemInList("Scanning Devices", "Ongoing")
        }
        mScannedDevices = arrayListOf()
        bleManager = getBleInstance()
        bleManager!!.startScan(bleScanCallback)
    }

    private fun updateStopButtonText(icon: Drawable, message: String) {
        viewBinder.stopButton.icon = icon
        viewBinder.stopButton.text = message
    }

    private fun isEndpointMatch(endpoint: String): Boolean {
        val isEndpointPresent = jobRunModel.workflowDevices?.find { it.deviceName == endpoint }
        return isEndpointPresent != null
    }

    // FixME [ mScannedDevices, ]
    private fun connectDevices() {
        // DummyData
        val mScannedDevices = ArrayList<DumBleDevice>()
        mScannedDevices.add(DumBleDevice("TestDev1", "DD:7E:7E:BD:AB:78", 22))
        //mScannedDevices.add(DumBleDevice("TestDev2", "FE:7E:7E:BD:AB:87", 25))
        //mScannedDevices.add(DumBleDevice("TestDev3", "DS:7E:7E:BD:AB:79", 20))
        //mScannedDevices.add(DumBleDevice("TestDev4", "XD:7E:7E:BD:AB:70", 20))

        val deviceCommands = getDeviceCommands(jobRunModel.workflowID, jobRunModel.workflowTasks)
        LogHelper.debug(TAG, "DeviceCommands Size: ${deviceCommands.size}")
        sdaViewModel.connectDevices(requireContext(),
            mScannedDevices,
            jobRunModel.workflowID,
            jobRunModel.sdaToken!!.accessToken,
            deviceCommands)
    }

    private fun finalizeEverything() {
        isJobCompleted = true
        viewBinder.tvDescription.text = resources.getString(R.string.stopped_text)
        viewBinder.iconView.setImageDrawable(
            PlatformUtils.fetchAttributeDrawable(
                requireContext(),
                R.attr.iconStop)
        )
        updateStopButtonText(
            resources.getDrawable(R.drawable.ic_check_light),
            resources.getString(R.string.finish_text)
        )
        viewBinder.elapsedTimer.stop()
    }

    private fun destroyObjects() {
        bleManager = null
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
