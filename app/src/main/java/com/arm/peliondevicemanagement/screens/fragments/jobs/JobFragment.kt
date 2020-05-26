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

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowDeviceAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemSwipeListener
import com.arm.peliondevicemanagement.components.listeners.SwipeDragControllerListener
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.components.models.workflow.device.DeviceRun
import com.arm.peliondevicemanagement.components.models.workflow.device.WorkflowDevice
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEFAULT_TIME_FORMAT
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.ExecutionMode
import com.arm.peliondevicemanagement.constants.state.NetworkErrorState
import com.arm.peliondevicemanagement.constants.state.workflow.WorkflowState
import com.arm.peliondevicemanagement.databinding.FragmentJobBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.screens.activities.ViewHostActivity
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.checkForLocationPermission
import com.arm.peliondevicemanagement.utils.PlatformUtils.enableBluetooth
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeColor
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeDrawable
import com.arm.peliondevicemanagement.utils.PlatformUtils.isBluetoothEnabled
import com.arm.peliondevicemanagement.utils.PlatformUtils.isLocationServiceEnabled
import com.arm.peliondevicemanagement.utils.PlatformUtils.openLocationServiceSettings
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeIntoTimeAgo
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseJSONTimeString
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getAudienceListFromDevices
import com.arm.peliondevicemanagement.utils.WorkflowUtils.getPermissionScopeFromTasks
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isValidSDAToken
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isWriteTaskAvailable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_job.*

class JobFragment : Fragment() {

    companion object {
        private val TAG: String = JobFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentJobBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: JobFragmentArgs by navArgs()

    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var workflowModel: Workflow
    private lateinit var workflowID: String
    private lateinit var workflowDeviceAdapter: WorkflowDeviceAdapter

    private var totalDevicesCompleted: Int = 0
    private var isSDATokenValid: Boolean = false
    private var _taskAssetState: AssetState =
        AssetState.NOT_REQUIRED
    private var isSyncInProgress: Boolean = false
    private lateinit var _downloadActionState: DownloadActionState

    private var errorBottomSheetDialog: BottomSheetDialog? = null
    private lateinit var retryButtonClickListener: View.OnClickListener

    private val swipeListener = object: RecyclerItemSwipeListener {
        override fun onSwipedLeft(position: Int) {
            LogHelper.debug(TAG, "Item swiped-left: $position")
            workflowDeviceAdapter.notifyItemChanged(position)
            if(workflowModel.workflowStatus == WorkflowState.PENDING.name){
                showSnackbar("Job sync needed, action denied")
            } else {
                verifyAndRunJob(position)
            }
        }
        override fun onSwipedRight(position: Int) {
            LogHelper.debug(TAG, "Item swiped-right: $position")
        }
    }

    enum class RunBundleState {
        PENDING_DEVICES,
        ALL_DEVICES,
        SPECIFIC_DEVICE
    }

    enum class AssetState {
        CHECKING,
        DOWNLOADING,
        DOWNLOADED,
        NOT_FOUND,
        NOT_REQUIRED
    }

    enum class DownloadActionState {
        SDA_DOWNLOAD,
        ASSET_DOWNLOAD,
        UNAUTHORIZED,
        JOB_SYNC
    }

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
        workflowID = args.workflowId
        setupData()
    }

    private fun setupData() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)
        // Fetch selected-workflow
        workflowViewModel.fetchSingleWorkflow(workflowID){ workflow ->
            LogHelper.debug(TAG, "Fetched from localCache of $workflowID")
            workflowModel = workflow
            setupEverything()
        }
    }

    private fun setupEverything() {
        requireActivity().runOnUiThread {
            workflowDeviceAdapter = WorkflowDeviceAdapter(workflowModel.workflowDevices!!)
            fetchCompletedDevicesCount()
            setupViews()
            setupListeners()
        }
    }

    private fun checkAndSetWorkflowCompleteStatus() {
        if(totalDevicesCompleted == workflowModel.workflowDevices?.size
            && (workflowModel.workflowStatus == WorkflowState.PENDING.name
                    || workflowModel.workflowStatus == WorkflowState.SYNCED.name)) {
            workflowViewModel.updateWorkflowStatus(workflowID, WorkflowState.COMPLETED.name)
            workflowModel.workflowStatus = WorkflowState.COMPLETED.name
            viewBinder.tvStatus.text = resources.getString(
                R.string.status_format, resources.getString(R.string.completed_text))
        }
    }

    private fun setupViews() {
        // Details Card
        viewBinder.tvName.text = workflowModel.workflowName
        viewBinder.tvDescription.text = workflowModel.workflowDescription
        // Set status
        when(workflowModel.workflowStatus){
            WorkflowState.SYNCED.name -> {
                viewBinder.tvStatus.text = resources.getString(
                    R.string.status_format, resources.getString(R.string.ready_text))
            }
            WorkflowState.PENDING.name -> {
                viewBinder.tvStatus.text = resources.getString(
                    R.string.status_format, resources.getString(R.string.pending_text))
            }
            WorkflowState.COMPLETED.name -> {
                viewBinder.tvStatus.text = resources.getString(
                    R.string.status_format, resources.getString(R.string.completed_text))
            }
            WorkflowState.REASSIGNED.name -> {
                viewBinder.tvStatus.text = resources.getString(
                    R.string.status_format, resources.getString(R.string.reassigned_text))
            }
        }
        viewBinder.tvDevices.text = requireContext().getString(
            R.string.total_devices_format, workflowModel.workflowDevices!!.size.toString())
        viewBinder.tvLocation.text = requireContext().getString(
            R.string.location_format, workflowModel.workflowLocation)
        val creationDateTime = parseJSONTimeString(workflowModel.workflowCreatedAt) +
                " - " + parseJSONTimeIntoTimeAgo(workflowModel.workflowCreatedAt)
        viewBinder.tvCreatedAt.text = requireContext().getString(
            R.string.created_at_format, creationDateTime)

        val totalCompletedText = "$totalDevicesCompleted/${workflowModel.workflowDevices!!.size}"

        viewBinder.tvCompleted.text = requireContext()
            .getString(R.string.devices_completed_format,
                totalCompletedText)

        viewBinder.rvDevices.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowDeviceAdapter
        }

        // Add swipe-gesture to devices list
        val itemTouchHelper = ItemTouchHelper(
            SwipeDragControllerListener(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_light)!!,
                fetchAttributeColor(requireContext(), R.attr.colorAccent), swipeListener,
                ItemTouchHelper.LEFT
            )
        )

        itemTouchHelper.attachToRecyclerView(viewBinder.rvDevices)
    }

    private fun setupListeners() {

        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
            when(_downloadActionState){
                DownloadActionState.SDA_DOWNLOAD -> {
                    refreshSDAToken()
                }
                DownloadActionState.ASSET_DOWNLOAD -> {
                    refreshAssets()
                }
                DownloadActionState.UNAUTHORIZED -> {
                    navigateToLogin()
                }
                DownloadActionState.JOB_SYNC -> {
                    processWorkflowSync()
                }
            }
        }

        workflowViewModel.getRefreshedSDAToken().observe(viewLifecycleOwner, Observer { tokenResponse ->
            if(!isSDATokenValid){
                workflowModel.sdaToken = tokenResponse
                workflowViewModel.updateLocalSDAToken(
                    workflowModel.workflowID, tokenResponse)
            }
            showHideSDAProgressbar(false)
            verifySDAToken()
        })

        workflowViewModel.getAssetAvailabilityStatus().observe(viewLifecycleOwner, Observer { state ->
            if(state){
                updateAssetView(AssetState.DOWNLOADED)
            } else {
                updateAssetView(AssetState.NOT_FOUND)
            }
        })

        workflowViewModel.getWorkflowSyncState().observe(viewLifecycleOwner, Observer { success ->
            if(success){
                if(workflowModel.workflowStatus != WorkflowState.COMPLETED.name){
                    LogHelper.debug(TAG, "Workflow synced successfully")
                    showSnackbar("Job synced successfully")
                    isSyncInProgress = false
                    workflowModel.workflowStatus = WorkflowState.SYNCED.name
                    workflowViewModel.updateWorkflowStatus(workflowID, WorkflowState.SYNCED.name)
                    verifySyncStatus()
                }
            } else {
                isSyncInProgress = false
                LogHelper.debug(TAG, "Workflow sync failed")
                showSnackbar("Job sync failed")
            }
        })

        workflowViewModel.getErrorResponseLiveData().observe(viewLifecycleOwner, Observer { error ->
            if(error != null) {
                when (error.errorCode) {
                    401 -> {
                        // Invalid-token
                        processErrorUnauthorized()
                    }
                    400 -> {
                        // Invalid-request
                        processErrorInvalidDevices()
                    }
                    else -> {
                        processErrorUnknown()
                    }
                }
            } else {
                processErrorUnknown()
            }
        })

        viewBinder.refreshTokenButton.setOnClickListener {
            refreshSDAToken()
        }

        viewBinder.tvDeviceHeader.setOnClickListener {
            expandCollapseDevicesRecView()
        }

        viewBinder.rvDevices.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if(dy>0){
                    viewBinder.runJobButton.hide()
                } else {
                    viewBinder.runJobButton.show()
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        viewBinder.runJobButton.setOnClickListener {
            if(workflowModel.workflowStatus == WorkflowState.PENDING.name){
                processWorkflowSync()
            } else {
                verifyAndRunJob()
            }
        }

        viewBinder.downloadButton.setOnClickListener {
            refreshAssets()
        }

        // Verify SDA-Token
        verifySDAToken()

        // If write-task available, then look for assets
        verifyAssetStatus()

        // If workflow-status is pending then, set the sync-button
        verifySyncStatus()

        // Check status for completion
        checkAndSetWorkflowCompleteStatus()
    }

    private fun verifyAssetStatus() {
        if(isWriteTaskAvailable(workflowModel.workflowTasks) && _taskAssetState != AssetState.DOWNLOADED){
            updateAssetView(AssetState.CHECKING)
            // Check for workflow-assets
            workflowViewModel.checkForWorkflowAssets(workflowID, workflowModel.workflowTasks)
        } else if(_taskAssetState == AssetState.DOWNLOADED) {
            updateAssetView(AssetState.DOWNLOADED)
        } else {
            updateAssetView(AssetState.NOT_REQUIRED)
        }
    }

    private fun verifySyncStatus() {
        if(workflowModel.workflowStatus == WorkflowState.PENDING.name){
            // Update run-button
            updateRunJobButtonText(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync_light)!!,
                resources.getString(R.string.sync_now_text)
            )
        } else {
            viewBinder.tvStatus.text = resources.getString(
                R.string.status_format, resources.getString(R.string.ready_text))
            // Update run-button
            updateRunJobButtonText(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_light)!!,
                resources.getString(R.string.run_job)
            )
        }
    }

    private fun processErrorUnauthorized() {
        showHideSDAProgressbar(false)
        verifySDAToken()

        setDownloadAction(DownloadActionState.UNAUTHORIZED)
        showErrorMessageDialog(NetworkErrorState.UNAUTHORIZED)
    }

    private fun processErrorInvalidDevices() {
        showHideSDAProgressbar(false)
        verifySDAToken()
        showSnackbar("Invalid devices, token not available")
    }

    private fun processErrorUnknown() {
        showHideSDAProgressbar(false)
        verifySDAToken()
        showSnackbar("Something went wrong")
    }

    private fun navigateToLogin() {
        (requireActivity() as ViewHostActivity).navigateToLogin()
    }

    private fun processWorkflowSync() {
        setDownloadAction(DownloadActionState.JOB_SYNC)
        if(!PlatformUtils.isNetworkAvailable(requireContext())) {
            showErrorMessageDialog(NetworkErrorState.NO_NETWORK)
            return
        }

        if(!isSDATokenValid){
            showSnackbar("Refresh secure device access, first")
            return
        }

        if(_taskAssetState != AssetState.NOT_REQUIRED){
            if(_taskAssetState != AssetState.DOWNLOADED){
                showSnackbar("Assets download required")
                return
            }
        }

        if(isSyncInProgress) {
            showSnackbar("Already working on it")
            return
        }

        isSyncInProgress = true
        showSnackbar("Syncing now")
        workflowViewModel.syncWorkflow(workflowID)
    }

    private fun setDownloadAction(state: DownloadActionState) {
        _downloadActionState = state
    }

    private fun setTaskAssetState(state: AssetState){
        _taskAssetState = state
    }

    private fun refreshAssets() {
        setDownloadAction(DownloadActionState.ASSET_DOWNLOAD)
        if(!PlatformUtils.isNetworkAvailable(requireContext())) {
            showErrorMessageDialog(NetworkErrorState.NO_NETWORK)
            return
        }

        updateAssetView(AssetState.DOWNLOADING)
        workflowViewModel.downloadWorkflowAssets(workflowID, workflowModel.workflowTasks)
    }

    private fun verifyAndRunJob(position: Int? = null) {
        if(isSDATokenValid){
            if(_taskAssetState == AssetState.NOT_REQUIRED || _taskAssetState == AssetState.DOWNLOADED){
                if(verifyBLEAndLocationPermissions()){
                    if(position != null){
                        initiateSpecificDeviceRun(position)
                    } else {
                        initiateJobRun()
                    }
                } else {
                    showSnackbar("Failed, try again")
                }
            } else {
                showSnackbar("Assets not downloaded")
            }
        } else {
            showSnackbar("Secure device access, not available")
        }
    }

    private fun showSnackbar(message: String) {
        (activity as ViewHostActivity).showSnackbar(
            viewBinder.root,message)
    }

    private fun fetchCompletedDevicesCount() {
        totalDevicesCompleted = 0
        workflowModel.workflowDevices?.forEach { device ->
            if(device.deviceState == DEVICE_STATE_COMPLETED){
                totalDevicesCompleted++
            }
        }
        LogHelper.debug(
            TAG, "completedDevices: $totalDevicesCompleted, " +
                "pendingDevices: ${workflowModel.workflowDevices!!.size - totalDevicesCompleted}")
    }

    private fun refreshSDAToken() {
        setDownloadAction(DownloadActionState.SDA_DOWNLOAD)
        if(!PlatformUtils.isNetworkAvailable(requireContext())){
            showErrorMessageDialog(NetworkErrorState.NO_NETWORK)
            return
        }

        showHideRefreshTokenButton(false)
        showHideSDAProgressbar(true)
        // Fetch permission-scope
        val permissionScope = getPermissionScopeFromTasks(workflowModel.workflowTasks)
        // Create audienceList
        val audienceList  = getAudienceListFromDevices(workflowModel.workflowDevices!!)
        // Call access-token request
        workflowViewModel.refreshSDAToken(permissionScope, audienceList)
    }

    private fun verifySDAToken() {
        if(workflowModel.sdaToken != null){
            val expiresIn = workflowModel.sdaToken!!.expiresIn
            val expiryDate = parseJSONTimeString(expiresIn)
            val expiryTime = parseJSONTimeString(expiresIn, DEFAULT_TIME_FORMAT)
            val expiryDateTime = "$expiryDate, $expiryTime"
            if(isValidSDAToken(expiresIn)){
                viewBinder.tvValidTill.text = requireContext().getString(
                    R.string.active_format, expiryDateTime)
                viewBinder.secureIconView.setImageDrawable(
                    fetchAttributeDrawable(requireContext(), R.attr.iconShieldGreen))
                showHideRefreshTokenButton(false)
                isSDATokenValid = true
            } else {
                viewBinder.tvValidTill.text = requireContext().getString(
                    R.string.expired_format, expiryDateTime)
                viewBinder.secureIconView.setImageDrawable(
                    fetchAttributeDrawable(requireContext(), R.attr.iconShieldRed))
                showHideRefreshTokenButton(true)
                isSDATokenValid = false
            }
        } else {
            viewBinder.tvValidTill.text = requireContext().getString(R.string.na)
            viewBinder.secureIconView.setImageDrawable(
                fetchAttributeDrawable(requireContext(), R.attr.iconShieldYellow))
            showHideRefreshTokenButton(true)
            isSDATokenValid = false
        }
    }

    private fun updateAssetView(state: AssetState){
        setTaskAssetState(state)
        when(state) {
            AssetState.CHECKING -> {
                viewBinder.tvAssetTitle.text = resources.getString(R.string.asset_checking_text)
                viewBinder.downloadButton.visibility = View.GONE
                viewBinder.assetStatusView.visibility = View.GONE
                viewBinder.downloadProgressbar.visibility = View.VISIBLE
                viewBinder.cardTaskAssetItem.visibility = View.VISIBLE
            }
            AssetState.DOWNLOADED -> {
                viewBinder.tvAssetTitle.text = resources.getString(R.string.asset_downloaded_text)
                viewBinder.downloadButton.visibility = View.GONE
                viewBinder.downloadProgressbar.visibility = View.GONE
                viewBinder.assetStatusView.visibility = View.VISIBLE
                viewBinder.cardTaskAssetItem.visibility = View.VISIBLE
            }
            AssetState.DOWNLOADING -> {
                viewBinder.tvAssetTitle.text = resources.getString(R.string.asset_downloading_text)
                viewBinder.downloadButton.visibility = View.GONE
                viewBinder.assetStatusView.visibility = View.GONE
                viewBinder.downloadProgressbar.visibility = View.VISIBLE
                viewBinder.cardTaskAssetItem.visibility = View.VISIBLE
            }
            AssetState.NOT_FOUND -> {
                viewBinder.tvAssetTitle.text = resources.getString(R.string.asset_not_found_text)
                viewBinder.downloadProgressbar.visibility = View.INVISIBLE
                viewBinder.assetStatusView.visibility = View.GONE
                viewBinder.downloadButton.visibility = View.VISIBLE
                viewBinder.cardTaskAssetItem.visibility = View.VISIBLE
            }
            AssetState.NOT_REQUIRED -> {
                viewBinder.cardTaskAssetItem.visibility = View.GONE
            }
        }
    }

    private fun updateRunJobButtonText(icon: Drawable, text: String) {
        viewBinder.runJobButton.icon = icon
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
                fetchAttributeDrawable(requireContext(), R.attr.iconArrowUp),
                null)
        } else {
            viewBinder.rvDevices.visibility = View.GONE
            viewBinder.tvDeviceHeader.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                fetchAttributeDrawable(requireContext(), R.attr.iconArrowDown),
                null)
        }
    }

    private fun showLocationServicesDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(resources.getString(R.string.attention_text))
            .setMessage(resources.getString(R.string.location_service_denied_desc))
            .setPositiveButton(resources.getString(R.string.open_settings_text)) { _, _ ->
                openLocationServiceSettings(context)
            }
            .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun verifyBLEAndLocationPermissions(): Boolean {
        // Enable feature-flag, if debug-build
        if(BuildConfig.DEBUG){
            val executionMode = WorkflowUtils.getSDAExecutionMode()
            if(executionMode == ExecutionMode.PHYSICAL){
                if(!isBluetoothEnabled()){
                    enableBluetooth(requireContext())
                    return false
                }
            }
        } else {
            if(!isBluetoothEnabled()){
                enableBluetooth(requireContext())
                return false
            }
        }
        // Check for permissions
        return if(checkForLocationPermission(requireActivity())){
            if(isLocationServiceEnabled(requireContext())){
                true
            } else {
                showLocationServicesDialog(requireContext())
                false
            }
        } else {
            false
        }
    }

    private fun initiateJobRun() {
        if(checkPendingDevices()){
            initiatePendingDevicesRun()
        } else {
            showWorkflowCompleteDialog()
        }
    }

    private fun initiatePendingDevicesRun() {
        val runBundle = createRunBundle(RunBundleState.PENDING_DEVICES)
        navigateToRunFragment(runBundle)
    }

    private fun initiateAllDevicesRun() {
        val runBundle = createRunBundle(RunBundleState.ALL_DEVICES)
        navigateToRunFragment(runBundle)
    }

    private fun initiateSpecificDeviceRun(position: Int) {
        val runBundle = createRunBundle(RunBundleState.SPECIFIC_DEVICE, position)
        navigateToRunFragment(runBundle)
    }

    private fun createRunBundle(state: RunBundleState, position: Int? = null): DeviceRun {
        return when(state){
            RunBundleState.PENDING_DEVICES -> {
                createBundleForPendingDevices()
            }
            RunBundleState.ALL_DEVICES -> {
                createBundleForAllDevices()
            }
            RunBundleState.SPECIFIC_DEVICE -> {
                createBundleForSpecificDevice(position!!)
            }
        }
    }

    private fun checkPendingDevices(): Boolean {
        return (workflowModel.workflowStatus != WorkflowState.COMPLETED.name
                && totalDevicesCompleted != workflowModel.workflowDevices?.size)
    }

    private fun createBundleForPendingDevices(): DeviceRun {
        // Construct listOf<Devices> which are pending or failed
        val pendingDevices = arrayListOf<WorkflowDevice>()
        workflowModel.workflowDevices?.forEach { device ->
            if(device.deviceState != DEVICE_STATE_COMPLETED){
                pendingDevices.add(device)
            }
        }

        LogHelper.debug(
            TAG, "createBundleForPendingDevices() " +
                "Found ${pendingDevices.size} pending-device")

        return DeviceRun(
            workflowModel.workflowID,
            workflowModel.workflowName,
            workflowModel.workflowLocation,
            workflowModel.workflowTasks, pendingDevices,
            workflowModel.sdaToken!!.accessToken
        )
    }

    private fun createBundleForAllDevices(): DeviceRun {
        val allDevices = ArrayList<WorkflowDevice>(workflowModel.workflowDevices!!)
        LogHelper.debug(
            TAG, "createRunBundleForAllDevices() " +
                "Found ${allDevices.size} device for run-bundle")

        return DeviceRun(
            workflowModel.workflowID,
            workflowModel.workflowName,
            workflowModel.workflowLocation,
            workflowModel.workflowTasks, allDevices,
            workflowModel.sdaToken!!.accessToken
        )
    }

    private fun createBundleForSpecificDevice(position: Int): DeviceRun {
        val device = workflowModel.workflowDevices?.get(position)
        LogHelper.debug(TAG, "createBundleForSpecificDevice() $device")
        return DeviceRun(
            workflowModel.workflowID,
            workflowModel.workflowName,
            workflowModel.workflowLocation,
            workflowModel.workflowTasks, arrayListOf(device!!),
            workflowModel.sdaToken!!.accessToken
        )
    }

    private fun navigateToRunFragment(runBundle: DeviceRun) {
        // Move to device-run
        Navigation.findNavController(viewBinder.root)
            .navigate(
                JobFragmentDirections.actionJobFragmentToJobRunFragment(
                    runBundle
                )
            )
    }

    private fun showWorkflowCompleteDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(resources.getString(R.string.job_completed_text))
            .setMessage(resources.getString(R.string.job_completed_desc))
            .setPositiveButton(resources.getString(R.string.re_run_text)) { _, _ ->
                initiateAllDevicesRun()
            }
            .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun showErrorMessageDialog(state: NetworkErrorState) {
        if(errorBottomSheetDialog != null) {
            // If previous dialog is already visible
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
        }

        when(state){
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

    override fun onDestroyView() {
        super.onDestroyView()
        LogHelper.debug(TAG, "->onDestroyView()")
        workflowViewModel.cancelAllRequests()
        _viewBinder = null
    }

}
