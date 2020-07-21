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

package com.arm.peliondevicemanagement.screens.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants.NAVIGATION_BACK_STATE_GRAPH
import com.arm.peliondevicemanagement.constants.AppConstants.VIEW_HOST_LAUNCH_GRAPH
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_ID_ARG
import com.arm.peliondevicemanagement.constants.AppConstants.viewHostLaunchActionList
import com.arm.peliondevicemanagement.constants.state.NavigationBackState
import com.arm.peliondevicemanagement.databinding.ActivityViewHostActivityBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.requestLocationPermission
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ViewHostActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var navigationController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewBinder: ActivityViewHostActivityBinding

    private lateinit var launchAction: String
    private var isJobRunGraph: Boolean = false
    private lateinit var navigationBackState: NavigationBackState

    companion object {
        private val TAG: String = ViewHostActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityViewHostActivityBinding.inflate(layoutInflater)
        initTheme(false)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Arguments
        launchAction = intent.getStringExtra(VIEW_HOST_LAUNCH_GRAPH)!!
        navigationBackState = NavigationBackState
            .valueOf(intent.getStringExtra(NAVIGATION_BACK_STATE_GRAPH)!!)

        // Initialize
        init()
    }

    private fun init() {
        toolbar = viewBinder.toolbar
        setupToolbar(toolbar, launchAction)

        navigationController = Navigation
            .findNavController(this, R.id.nav_host_fragment)
        val navGraph = navigationController.navInflater.inflate(R.navigation.view_host_nav_graph)
        when(launchAction){
            viewHostLaunchActionList[0] -> {
                navGraph.startDestination = R.id.jobFragment
                val args = NavArgument.Builder()
                    .setDefaultValue(intent.getStringExtra(WORKFLOW_ID_ARG))
                    .build()
                navGraph.addArgument(WORKFLOW_ID_ARG, args)
            }
            viewHostLaunchActionList[1] -> {
                navGraph.startDestination = R.id.settingsFragment
            }
        }
        navigationController.graph = navGraph
        appBarConfiguration = AppBarConfiguration.Builder().build()
        NavigationUI.setupActionBarWithNavController(this, navigationController, appBarConfiguration)

        setupListeners()
    }

    private fun setupListeners() {
        navigationController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id){
                R.id.jobFragment -> {
                    updateToolbarText(getString(R.string.job_text))
                    isJobRunGraph = false
                }
                R.id.jobRunFragment -> {
                    updateToolbarText(getString(R.string.job_run_text))
                    isJobRunGraph = true
                }
                R.id.settingsFragment -> {
                    updateToolbarText(getString(R.string.settings))
                }
                R.id.activityInfoFragment -> {
                    updateToolbarText(getString(R.string.login_history_text))
                }
                R.id.helpAndSupportFragment -> {
                    updateToolbarText(getString(R.string.help_and_support))
                }
                R.id.licensesFragment -> {
                    updateToolbarText(getString(R.string.lib_we_use_text))
                }
                R.id.licenseViewFragment -> {
                    updateToolbarText(getString(R.string.license_text))
                }
                R.id.webViewFragment -> {
                    updateToolbarText(getString(R.string.in_app_browse_text))
                }
                R.id.developerOptionsFragment -> {
                    updateToolbarText(getString(R.string.developer_options_text))
                }
            }
        }
    }

    private fun updateToolbarText(titleText: String) {
        toolbar.apply {
            title = titleText
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return processNavigation()
    }

    override fun onBackPressed() {
        processNavigation()
    }

    private fun processNavigation(): Boolean {
        return if(isJobRunGraph) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            if(!NavigationUI.navigateUp(navigationController, appBarConfiguration)){
                navigateBackToRelevantActivity()
            }
            return true
        }
    }

    private fun navigateBackToRelevantActivity() {
        when(navigationBackState){
            NavigationBackState.CHOOSE_FEATURES -> {
                LogHelper.debug(TAG, "-> navigateBackToChooseFeatureActivity()")
                fireIntentWithFinish(
                    Intent(this, ChooseFeatureActivity::class.java),
                    false)
            }
            NavigationBackState.JOB_MANAGEMENT -> {
                LogHelper.debug(TAG, "-> navigateBackToJobManagementActivity()")
                fireIntentWithFinish(
                    Intent(this, JobManagementActivity::class.java),
                    false)
            }
            NavigationBackState.DEVICE_MANAGEMENT -> {
                LogHelper.debug(TAG, "-> navigateBackToDevicesManagementActivity()")
                fireIntentWithFinish(
                    Intent(this, DeviceManagementActivity::class.java),
                    false)
            }
        }
    }

    fun navigateToLogin() {
        LogHelper.debug(TAG, "Temporary sign-out complete")
        fireIntentWithFinish(Intent(this, AuthActivity::class.java), false)
    }

    @RequiresApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PlatformUtils.REQUEST_PERMISSION -> if(grantResults.isNotEmpty()) {
                val locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if(!locationAccepted){
                    if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.attention_text))
                            .setMessage(resources.getString(R.string.location_perm_desc))
                            .setPositiveButton(resources.getString(R.string.grant_text)) { _, _ ->
                                requestLocationPermission(this)
                            }
                            .setNegativeButton(resources.getString(R.string.deny_text)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.attention_text))
                            .setMessage(resources.getString(R.string.location_perm_denied_desc))
                            .setPositiveButton(resources.getString(R.string.open_settings_text)) { _, _ ->
                                PlatformUtils.openAppSettings(this)
                            }
                            .setNegativeButton(resources.getString(R.string.cancel_text)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    }
                }
            }
        }
    }

}
