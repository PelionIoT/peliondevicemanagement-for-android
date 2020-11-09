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

package com.arm.peliondevicemanagement.screens.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.FeatureAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.FeatureModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_MANAGEMENT
import com.arm.peliondevicemanagement.constants.AppConstants.JOB_MANAGEMENT
import com.arm.peliondevicemanagement.constants.state.NavigationBackState
import com.arm.peliondevicemanagement.databinding.ActivityChooseFeatureBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeDrawable
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.layout_drawer_header.view.*

class ChooseFeatureActivity : BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    RecyclerItemClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerHeaderView: View
    private lateinit var navigationView: NavigationView

    private lateinit var viewBinder: ActivityChooseFeatureBinding

    private val featureList= arrayListOf<FeatureModel>()
    private lateinit var featureAdapter: FeatureAdapter

    companion object {
        private val TAG: String = ChooseFeatureActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityChooseFeatureBinding.inflate(layoutInflater)
        initTheme(false)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
    }

    private fun init() {
        toolbar = viewBinder.toolbar
        drawerLayout = viewBinder.drawerLayout
        drawerHeaderView = viewBinder.navigationView
            .inflateHeaderView(R.layout.layout_drawer_header)
        navigationView = viewBinder.navigationView

        // In-case of single-account, update drawer-menu
        if(!SharedPrefHelper.isMultiAccountSupported()){
            navigationView.menu.clear()
            navigationView.inflateMenu(R.menu.menu_single_account)
        }

        setupToolbar(toolbar, getString(R.string.choose_features_text))
        updateDrawerViews()

        val drawerToggle = ActionBarDrawerToggle(this,
            drawerLayout, toolbar,
            R.string.na, R.string.na)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        setupItemNavViewColors()

        drawerLayout.background = PlatformUtils
            .fetchAttributeColor(this, R.attr.colorBackground)

        addFeatures()
        featureAdapter = FeatureAdapter(featureList = featureList, itemClickListener = this)
        viewBinder.rvFeatures.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = featureAdapter
        }
    }

    private fun addFeatures() {
        featureList.clear()
        // Add features here
        featureList.add(
            FeatureModel(
                fetchAttributeDrawable(this, R.attr.iconWorkflowManagement),
                getString(R.string.feature_job_management_text),
                getString(R.string.feature_job_management_desc)
            )
        )
        featureList.add(
            FeatureModel(
                fetchAttributeDrawable(this, R.attr.iconDeviceManagement),
                getString(R.string.feature_device_management_text),
                getString(R.string.feature_device_management_desc)
            )
        )
    }

    private fun setupItemNavViewColors() {
        val colorList: ColorStateList = if(SharedPrefHelper.isDarkThemeEnabled()){
            ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white))
        } else {
            ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
        }
        navigationView.itemTextColor = colorList
        navigationView.itemIconTintList = colorList
        navigationView.background = PlatformUtils.fetchAttributeColor(this, R.attr.cardColor)
    }

    private fun updateDrawerViews() {
        // For User-View
        if(SharedPrefHelper.getUserName().isEmpty()){
            drawerHeaderView.tvUserEmail.text = resources.getString(R.string.na)
        } else {
            drawerHeaderView.tvUserEmail.text = SharedPrefHelper.getUserName()
        }
        // For Account-View
        if(!SharedPrefHelper.getSelectedAccountID().isBlank() &&
            !SharedPrefHelper.getSelectedAccountName().isBlank()){
            drawerHeaderView.tvUserAccountName.text =
                SharedPrefHelper.getSelectedAccountName()
            drawerHeaderView.accountHeader.visibility = View.VISIBLE
        } else {
            drawerHeaderView.accountHeader.visibility = View.GONE
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        drawerLayout.closeDrawers()

        when(item.itemId){
            R.id.switch_account -> {
                LogHelper.debug(TAG, "->switchAccount()")
                navigateToAccounts()
            }
            R.id.settings -> {
                LogHelper.debug(TAG, "->settings()")
                navigateToSettings()
            }
            R.id.signout -> {
                LogHelper.debug(TAG, "->signOut()")
                navigateToLogin()
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            callCloseApp()
        }
    }

    private fun navigateToAccounts() {
        val accountIntent = Intent(this, AuthActivity::class.java)
        accountIntent.putExtra(AppConstants.IS_ACCOUNT_GRAPH, true)
        fireIntentWithFinish(accountIntent, false)
    }

    private fun navigateToLogin() {
        WorkflowUtils.deleteWorkflowsCache()
        SharedPrefHelper.storeMultiAccountStatus(false)
        SharedPrefHelper.clearEverything()
        LogHelper.debug(TAG, "Sign-out complete")
        fireIntentWithFinish(Intent(this, AuthActivity::class.java), false)
    }

    private fun navigateToSettings() {
        val settingsIntent = Intent(this, ViewHostActivity::class.java)
        settingsIntent.putExtra(AppConstants.NAVIGATION_BACK_STATE_GRAPH, NavigationBackState.CHOOSE_FEATURES.name)
        settingsIntent.putExtra(AppConstants.VIEW_HOST_LAUNCH_GRAPH, AppConstants.viewHostLaunchActionList[1])
        fireIntentWithFinish(settingsIntent, true)
    }

    private fun navigateToJobManagementActivity() {
        fireIntentWithFinish(Intent(this, JobManagementActivity::class.java), true)
    }

    private fun navigateToDeviceManagementActivity() {
        fireIntentWithFinish(Intent(this, DeviceManagementActivity::class.java), true)
    }

    override fun onItemClick(data: Any) {
        val featureName = data as String
        LogHelper.debug(TAG, "onItemClick()-> featureName: $featureName")
        if(featureName == JOB_MANAGEMENT) {
            navigateToJobManagementActivity()
        } else if(featureName == DEVICE_MANAGEMENT){
            navigateToDeviceManagementActivity()
        }
    }
}