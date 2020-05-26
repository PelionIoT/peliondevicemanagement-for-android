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

package com.arm.peliondevicemanagement.screens.fragments.jobs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.bluetooth.le.ScanResult
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.models.GenericBleDevice
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceResponse
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceRun
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.models.workflow.task.TaskRun
import com.arm.peliondevicemanagement.components.viewmodels.SDAViewModel
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_OUT_ASSETS_FILENAME
import com.arm.peliondevicemanagement.constants.ExecutionMode
import com.arm.peliondevicemanagement.constants.state.workflow.device.DeviceResponseState
import com.arm.peliondevicemanagement.constants.state.workflow.device.DeviceScanState
import com.arm.peliondevicemanagement.constants.state.workflow.device.DeviceState
import com.arm.peliondevicemanagement.constants.state.workflow.task.TaskRunState
import com.arm.peliondevicemanagement.databinding.FragmentJobRunBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.getBleInstance
import com.arm.peliondevicemanagement.utils.WorkflowUtils.createDeviceRunLog
import com.arm.peliondevicemanagement.utils.WorkflowUtils.createTaskRunLog
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getDeviceCommands
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getSDAExecutionMode
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isReadTaskType
import com.arm.pelionmobiletransportsdk.ble.BleDevice
import com.arm.pelionmobiletransportsdk.ble.callbacks.BleScannerCallback
import com.arm.pelionmobiletransportsdk.ble.scanner.BleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class JobRunFragment : Fragment() {

    companion object {
        private val TAG: String = JobRunFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobRunBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobRunFragmentArgs by navArgs()

    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var sdaViewModel: SDAViewModel

    private lateinit var deviceRunModel: DeviceRun
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private var totalDevicesCompleted: Int = 0
    private var activeItemPosition: Int = 0
    private var taskFailureCount: Int = 0
    private lateinit var taskRunLogs: ArrayList<TaskRun>

    private var isScanCompleted: Boolean = false
    private var isJobCompleted: Boolean = false

    private var bleManager: BleManager? = null
    private lateinit var mScannedDevices: ArrayList<GenericBleDevice>

    private val bleScanCallback: BleScannerCallback = object: BleScannerCallback {
        override fun onBatchScanResults(results: List<ScanResult>) {
            // Do nothing
        }

        override fun onFinish() {
            LogHelper.debug(TAG, "BleScan->onFinish()")
            if(mScannedDevices.isNotEmpty()){
                isScanCompleted = true
                updateStopButtonText(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_stop_light)!!,
                    resources.getString(R.string.stop_text)
                )
                removeTemporaryDeviceItemFromList()
                connectDevices()
            } else {
                isScanCompleted = false
                addTemporaryDeviceItemInList("No devices found", DeviceScanState.FAILED)
                updateStopButtonText(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_refresh_light)!!,
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
                mScannedDevices.add(
                    GenericBleDevice(
                        bleDevice.device.name,
                        bleDevice.deviceAddress,
                        bleDevice.deviceRSSI)
                )
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
        deviceRunModel = args.runObject
        //LogHelper.debug(TAG, "deviceRunBundle: $deviceRunModel")

        setupData()
        setupViews()
        setupListeners()
    }

    private fun setupData() {
        taskRunLogs = arrayListOf()
        sdaViewModel = ViewModelProvider(this).get(SDAViewModel::class.java)
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)
        workflowDeviceAdapter = WorkflowDeviceAdapter(deviceRunModel.workflowDevices)

        // If debug-build, apply this feature-flag
        if(BuildConfig.DEBUG){
            val executionMode = getSDAExecutionMode()
            sdaViewModel.setExecutionMode(executionMode)

            when(executionMode){
                ExecutionMode.PHYSICAL -> {
                    setupScan()
                }
                ExecutionMode.VIRTUAL -> {
                    setupDummyDevicesForRun()
                }
            }
        } else {
            setupScan()
        }
    }

    private fun setTotalDevicesCompletedStatus(){
        totalDevicesCompleted = 0
        deviceRunModel.workflowDevices.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(
            TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${deviceRunModel.workflowDevices.size.minus(totalDevicesCompleted)}")

        val completedOutOfPending = "$totalDevicesCompleted/${deviceRunModel.workflowDevices.size}"
        viewBinder.tvCompleted.text = resources.getString(
            R.string.devices_completed_format,
            completedOutOfPending
        )
    }

    private fun setupViews() {
        viewBinder.tvName.text = deviceRunModel.workflowName
        viewBinder.tvTasks.text = requireContext().getString(
            R.string.total_tasks_format,
            deviceRunModel.workflowTasks.size.toString())
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

        sdaViewModel.deviceStateLiveData.observe(viewLifecycleOwner, Observer { stateResponse ->
            when(stateResponse.state) {
                DeviceState.CONNECTING -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Connecting, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    addTemporaryDeviceItemInList(stateResponse.deviceIdentifier, DeviceScanState.CONNECTING)
                }
                DeviceState.CONNECTED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Connected, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                }
                DeviceState.RUNNING -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Running, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Running")
                }
                DeviceState.COMMAND_COMPLETED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Command-Completed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                }
                DeviceState.COMMAND_FAILED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Command-Failed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                }
                DeviceState.COMPLETED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Completed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    updateDeviceItemInList(activeItemPosition, stateResponse.deviceIdentifier, "Completed")
                    processDeviceRunLogs(stateResponse.deviceIdentifier)
                    sdaViewModel.disconnectFromDevice()
                }
                DeviceState.DISCONNECTED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Disconnected, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    activeItemPosition = 0
                    setTotalDevicesCompletedStatus()
                }
                DeviceState.FAILED -> {
                    LogHelper.debug(
                        TAG, "DeviceState: Failed, " +
                            "deviceIdentifier: ${stateResponse.deviceIdentifier}")
                    sdaViewModel.disconnectFromDevice()
                }
            }
        })

        sdaViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { deviceResponse ->
            //LogHelper.debug(TAG, "DeviceResponse: $deviceResponse")
            when(deviceResponse.responseState){
                DeviceResponseState.SDA -> {
                    // Process SDA response
                    processSDAResponse(deviceResponse)
                }
                DeviceResponseState.ENDPOINT -> {
                    // Process endpoint response
                    processEndpointResponse(deviceResponse.response!!)
                }
                DeviceResponseState.JOB_COMPLETED -> {
                    LogHelper.debug(TAG, "All devices are now completed")
                    finalizeEverything()
                }
            }
        })
    }

    private fun processSDAResponse(deviceResponse: DeviceResponse) {
        val taskRunLog: TaskRun = if(deviceResponse.operationResponse != null){
            // If task-type is READ then save command-response in logs
            if(isReadTaskType(deviceResponse.taskID!!, deviceRunModel.workflowTasks)
                && deviceResponse.operationResponse.blob != null){
                // Parse command-response
                val outputResponse = deviceResponse.operationResponse.blob!!
                LogHelper.debug(TAG, "SDA_Command_Response ${outputResponse.contentToString()}")
                // Save to local-storage
                workflowViewModel.saveWorkflowTaskOutputAssets(deviceRunModel.workflowID,
                    deviceResponse.taskID, outputResponse)
                // Create run-log
                createTaskRunLog(deviceResponse.taskID,
                    TaskRunState.SUCCEEDED, WORKFLOW_OUT_ASSETS_FILENAME)
            } else {
                // Create run-log
                createTaskRunLog(deviceResponse.taskID, TaskRunState.SUCCEEDED)
            }
            //LogHelper.debug(TAG, "TaskRunLogs: $taskRunLog")
        } else {
            taskFailureCount++
            // Create run-log
            createTaskRunLog(deviceResponse.taskID!!, TaskRunState.FAILED)
            //LogHelper.debug(TAG, "TaskRunLogs: $taskRunLog")
        }
        taskRunLogs.add(taskRunLog)
        LogHelper.debug(TAG, "Task-log saved successfully")
        sdaViewModel.setCommandLock(locked = false)
    }

    private fun processEndpointResponse(endpoint: String) {
        removeTemporaryDeviceItemFromList()
        if(isEndpointMatch(endpoint)){
            // Go forward and execute job
            LogHelper.debug(TAG, "Endpoint matched: $endpoint, ->runJob()")
            GlobalScope.launch {
                activeItemPosition = getDevicePosition(endpoint)
                taskFailureCount = 0
                taskRunLogs = arrayListOf()
                // Now run-job
                sdaViewModel.runJob()
            }
        } else {
            LogHelper.debug(TAG, "Endpoint not matched: $endpoint")
            // Terminate connection
            sdaViewModel.disconnectFromDevice()
        }
    }

    private fun processDeviceRunLogs(deviceID: String) {
        val currentDateTime = PlatformUtils.getCurrentTimeInZFormat()
        val deviceRunLog = createDeviceRunLog(
            deviceRunModel.workflowID,
            deviceID,
            deviceRunModel.workflowLocation,
            currentDateTime, "The command ran to completion without any hassle",
            taskRunLogs, taskFailureCount)

        val devicePosition = getItemPosition(deviceID)
        deviceRunModel.workflowDevices[devicePosition].deviceRunLogs = deviceRunLog
        LogHelper.debug(TAG, "DeviceRunLog: ${deviceRunModel.workflowDevices[devicePosition].deviceRunLogs}")
        LogHelper.debug(TAG, "Run-log created successfully")
    }

    private fun addTemporaryDeviceItemInList(deviceText: String, deviceState: DeviceScanState, failedCount: Int? = null){
        val scanItem = viewBinder.scanDeviceItem
        scanItem.root.alpha = 1.0f
        when(deviceState){
            DeviceScanState.ONGOING -> {
                scanItem.tvName.text = deviceText
                scanItem.tvDescription.text = resources.getString(R.string.ongoing_text)
                scanItem.viewDeviceStatus.visibility = View.GONE
                scanItem.viewProgressbar.visibility = View.VISIBLE
            }
            DeviceScanState.FAILED -> {
                if (failedCount != null) {
                    when {
                        (failedCount > 1) -> {
                            scanItem.tvName.text = resources.getString(R.string.failed_devices_format, failedCount.toString())
                        }
                        failedCount == 1 -> {
                            scanItem.tvName.text = resources.getString(R.string.failed_device_format, failedCount.toString())
                        }
                    }
                } else {
                    scanItem.tvName.text = deviceText
                }
                scanItem.tvDescription.text = resources.getString(R.string.failed_text)
                scanItem.viewDeviceStatus.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_status_failed)
                scanItem.viewDeviceStatus.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_exclamation))
                scanItem.viewProgressbar.visibility = View.INVISIBLE
                scanItem.viewDeviceStatus.visibility = View.VISIBLE
            }
            DeviceScanState.CONNECTING -> {
                scanItem.tvName.text = deviceText
                scanItem.tvDescription.text = resources.getString(R.string.connecting_text)
                scanItem.viewDeviceStatus.visibility = View.GONE
                scanItem.viewProgressbar.visibility = View.VISIBLE
            }
        }
        scanItem.root.visibility = View.VISIBLE
    }

    private fun updateDeviceItemInList(position: Int, deviceName: String, deviceState: String){
        deviceRunModel.workflowDevices[position] = WorkflowDevice(deviceName, deviceState)
        workflowDeviceAdapter.notifyItemChanged(position)
    }

    private fun removeTemporaryDeviceItemFromList() {
        val scanItem = viewBinder.scanDeviceItem.root
        scanItem.animate()
            .alpha(0.0f)
            .translationY(0f)
            .setDuration(300)
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    viewBinder.scanDeviceItem.root.visibility = View.GONE
                }
            })
    }

    private fun getItemPosition(deviceIdentifier: String): Int {
        return deviceRunModel.workflowDevices.indexOf(
            WorkflowDevice(deviceIdentifier, "Pending")
        )
    }

    private suspend fun getDevicePosition(deviceIdentifier: String): Int
            = withContext(Dispatchers.IO){
        return@withContext deviceRunModel.workflowDevices.indexOf(
            WorkflowDevice(deviceIdentifier, "Pending")
        )
    }

    private fun setupScan() {
        addTemporaryDeviceItemInList("Scanning devices", DeviceScanState.ONGOING)
        mScannedDevices = arrayListOf()
        bleManager = getBleInstance()
        bleManager!!.startScan(bleScanCallback)
    }

    private fun updateStopButtonText(icon: Drawable, message: String) {
        viewBinder.stopButton.icon = icon
        viewBinder.stopButton.text = message
    }

    private fun isEndpointMatch(endpoint: String): Boolean {
        val isEndpointPresent = deviceRunModel.workflowDevices.find { it.deviceName == endpoint }
        return isEndpointPresent != null
    }

    private fun connectDevices() {
        val deviceCommands = getDeviceCommands(deviceRunModel.workflowID, deviceRunModel.workflowTasks)
        LogHelper.debug(TAG, "DeviceCommands Size: ${deviceCommands.size}")
        sdaViewModel.connectDevices(requireContext(),
            mScannedDevices,
            deviceRunModel.workflowID,
            deviceRunModel.workflowSDAToken,
            deviceCommands)
    }

    private fun setupDummyDevicesForRun() {
        // Mark scan as completed
        isScanCompleted = true

        // Add dummy-devices to workflowDevices-list
        /*deviceRunModel.workflowDevices.add(
            WorkflowDevice("026eead293eb926ca57ba92703c00000", "Pending")
        )
        deviceRunModel.workflowDevices.add(
            WorkflowDevice("036eead293eb926ca57ba92703c00000", "Pending")
        )*/

        // Add dummy-devices data to mScannedDevices-list
        mScannedDevices = arrayListOf()
        mScannedDevices.add(
            GenericBleDevice("TestDev1", "DD:7E:7E:BD:AB:78", 22)
        )
        //mScannedDevices.add(
        //   GenericBleDevice("TestDev2", "FE:7E:7E:BD:AB:87", 25)
        //)
        /*mScannedDevices.add(
            GenericBleDevice("TestDev3", "XD:7E:7E:BD:AB:70", 20)
        )
        mScannedDevices.add(
            GenericBleDevice("TestDev4", "DS:7E:7E:BD:AB:79", 20)
        )*/

        // Now initiate workflow-run
        connectDevices()
    }

    private fun finalizeEverything() {
        var failedCount = 0
        deviceRunModel.workflowDevices.forEach { device ->
            if(device.deviceState != DEVICE_STATE_COMPLETED){
                failedCount++
            }
        }
        if(failedCount > 0){
            addTemporaryDeviceItemInList("", DeviceScanState.FAILED, failedCount)
        }

        // Update localDB for changes
        workflowViewModel.updateWorkflowDevices(
            deviceRunModel.workflowID, deviceRunModel.workflowDevices) {
            LogHelper.debug(TAG, "Devices updated in local-cache.")
        }

        viewBinder.tvDescription.text = resources.getString(R.string.stopped_text)
        viewBinder.iconView.setImageDrawable(
            PlatformUtils.fetchAttributeDrawable(
                requireContext(),
                R.attr.iconStop)
        )
        updateStopButtonText(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_light)!!,
            resources.getString(R.string.finish_text)
        )
        viewBinder.elapsedTimer.stop()

        isJobCompleted = true
    }

    private fun destroyObjects() {
        bleManager = null
        workflowViewModel.cancelAllRequests()
        sdaViewModel.cancelAllRequests()
        _viewBinder = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyObjects()
    }

    private fun showCautionDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(resources.getString(R.string.attention_text))
            .setMessage(resources.getString(R.string.job_cancel_text))
            .setPositiveButton(resources.getString(R.string.yes_stop_text)) { _, _ ->
                Navigation.findNavController(viewBinder.root).navigateUp()
            }
            .setNegativeButton(resources.getString(R.string.no_text)) { dialogInterface, _ ->
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
