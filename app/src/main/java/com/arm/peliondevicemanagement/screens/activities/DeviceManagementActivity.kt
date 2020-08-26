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

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.ViewPagerAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.ACTIVITY_RESULT
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICES_AND_ENROLLING_SEARCH
import com.arm.peliondevicemanagement.constants.AppConstants.SCAN_QR_REQUEST_CODE
import com.arm.peliondevicemanagement.constants.state.NavigationBackState
import com.arm.peliondevicemanagement.constants.state.devices.DevicesSearchState
import com.arm.peliondevicemanagement.databinding.ActivityDeviceManagementBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.fragments.devices.DevicesFragment
import com.arm.peliondevicemanagement.screens.fragments.devices.EnrollingDevicesFragment
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.layout_drawer_header.view.*

class DeviceManagementActivity : BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener, RecyclerItemClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerHeaderView: View
    private lateinit var navigationView: NavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private lateinit var viewBinder: ActivityDeviceManagementBinding

    private var devicesFragment: DevicesFragment? = null
    private var enrollingDevicesFragment: EnrollingDevicesFragment? = null

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var fragmentList: List<Fragment>
    private lateinit var fragmentNamesList: List<String>

    private var activeSearchState: DevicesSearchState = DevicesSearchState.DEVICES

    companion object {
        private val TAG: String = DeviceManagementActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme(false)
        super.onCreate(savedInstanceState)
        viewBinder = ActivityDeviceManagementBinding.inflate(layoutInflater)
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
            navigationView.inflateMenu(R.menu.menu_single_feature)
        }

        viewPager = viewBinder.contentView.viewPager
        tabLayout = viewBinder.contentView.tabLayout

        setupToolbar(toolbar, getString(R.string.devices_text))
        updateDrawerViews()

        val drawerToggle = ActionBarDrawerToggle(this,
            drawerLayout, toolbar,
            R.string.na, R.string.na)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        viewBinder.searchBar.setOnClickListener {
            startSearchActivity(activeSearchState)
        }

        setupItemNavViewColors()
        setupTabLayoutColors()

        initFragmentViews()
        setupViewPagerWithTabs()
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

    private fun setupTabLayoutColors() {
        tabLayout.background = PlatformUtils.fetchAttributeDrawable(this, R.attr.cardColor)
        val normalColor: Int
        val selectedColor: Int
        val rippleColor: ColorStateList
        if(SharedPrefHelper.isDarkThemeEnabled()){
            normalColor = ContextCompat.getColor(this, android.R.color.white)
            selectedColor = ContextCompat.getColor(this, R.color.colorAccentForDark)
            rippleColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccentForDark))
            toolbar.elevation = 0f
        } else {
            normalColor = ContextCompat.getColor(this, android.R.color.black)
            selectedColor = ContextCompat.getColor(this, R.color.colorAccentForLight)
            rippleColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccentForLight))
        }

        tabLayout.setTabTextColors(normalColor, selectedColor)
        tabLayout.setSelectedTabIndicatorColor(selectedColor)
        tabLayout.tabRippleColor = rippleColor

        // Define ColorStateList
        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(-android.R.attr.state_selected)
        )
        val colors = intArrayOf(
            selectedColor,
            normalColor
        )
        tabLayout.tabIconTint = ColorStateList(states, colors)
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

    fun startSearchActivity(state: DevicesSearchState) {
        val searchIntent = Intent(this, SearchDevicesEnrollingActivity::class.java)
        when(state) {
            DevicesSearchState.DEVICES -> {
                searchIntent.putExtra(DEVICES_AND_ENROLLING_SEARCH, DevicesSearchState.DEVICES.name)
            }
            DevicesSearchState.ENROLLING_DEVICES -> {
                searchIntent.putExtra(DEVICES_AND_ENROLLING_SEARCH, DevicesSearchState.ENROLLING_DEVICES.name)
            }
        }
        fireIntentWithFinish(searchIntent, true)
    }

    private fun initFragmentViews() {
        fragmentNamesList = listOf(
            getString(R.string.devices_text),
            getString(R.string.enrolling_devices_text)
        )
        // Initialize fragments
        devicesFragment =
            DevicesFragment()
        enrollingDevicesFragment =
            EnrollingDevicesFragment()
        // Add them to the list
        fragmentList = listOf(devicesFragment!!, enrollingDevicesFragment!!)
        // Initialize adapter
        viewPagerAdapter = ViewPagerAdapter(this, fragmentList)
        // Add adapter to view-pager
        viewPager.adapter = viewPagerAdapter
        viewPager.isUserInputEnabled = false
    }

    private fun setupViewPagerWithTabs() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if(fragmentNamesList[position] == "Devices"){
                tab.icon = PlatformUtils
                    .fetchAttributeDrawable(this, R.attr.iconIoTDevice)
            } else {
                tab.icon = PlatformUtils
                    .fetchAttributeDrawable(this, R.attr.iconEnrollDevices)
            }
            tab.text = fragmentNamesList[position]
        }.attach()

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                activeSearchState = if(tab.position == 0) {
                    DevicesSearchState.DEVICES
                } else {
                    DevicesSearchState.ENROLLING_DEVICES
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        drawerLayout.closeDrawers()

        when(item.itemId){
            R.id.switch_account -> {
                LogHelper.debug(TAG, "->switchAccount()")
                navigateToAccounts()
            }
            R.id.change_feature -> {
                LogHelper.debug(TAG, "->changeFeature()")
                navigateToChooseFeature()
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

    private fun navigateToChooseFeature() {
        fireIntentWithFinish(Intent(this, ChooseFeatureActivity::class.java), false)
    }

    fun navigateToEnrollQRScan() {
        startActivityForResult(
            Intent(this, EnrollQRScanActivity::class.java),
            SCAN_QR_REQUEST_CODE
        )
    }

    private fun navigateToLogin() {
        WorkflowUtils.deleteWorkflowsCache()
        SharedPrefHelper.storeMultiAccountStatus(false)
        SharedPrefHelper.clearEverything()
        LogHelper.debug(TAG, "Sign-out complete")
        fireIntentWithFinish(Intent(this, AuthActivity::class.java), false)
    }

    fun initiateTemporarySignOut() {
        LogHelper.debug(TAG, "Temporary sign-out complete")
        fireIntentWithFinish(Intent(this, AuthActivity::class.java), false)
    }

    private fun navigateToSettings() {
        val settingsIntent = Intent(this, ViewHostActivity::class.java)
        settingsIntent.putExtra(AppConstants.NAVIGATION_BACK_STATE_GRAPH, NavigationBackState.DEVICE_MANAGEMENT.name)
        settingsIntent.putExtra(AppConstants.VIEW_HOST_LAUNCH_GRAPH, AppConstants.viewHostLaunchActionList[1])
        fireIntentWithFinish(settingsIntent, true)
    }

    override fun onItemClick(data: Any) {
        // ToDO
        LogHelper.debug(TAG, "->navigateToDashboard() [ TODO ]")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if (requestCode == SCAN_QR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = data!!.getBooleanExtra(ACTIVITY_RESULT,false)
                if(result){
                    LogHelper.debug(TAG, "Requesting refresh for enrolling-devices list")
                    enrollingDevicesFragment!!.refreshContent()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        devicesFragment = null
        enrollingDevicesFragment = null
    }
}