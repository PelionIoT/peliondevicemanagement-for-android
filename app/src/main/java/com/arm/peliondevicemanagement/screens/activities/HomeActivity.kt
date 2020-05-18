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
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.ViewPagerAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.IS_ACCOUNT_GRAPH
import com.arm.peliondevicemanagement.constants.AppConstants.VIEW_HOST_LAUNCH_GRAPH
import com.arm.peliondevicemanagement.constants.AppConstants.viewHostLaunchActionList
import com.arm.peliondevicemanagement.databinding.ActivityHomeBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.fragments.jobs.CompletedJobsFragment
import com.arm.peliondevicemanagement.screens.fragments.jobs.PendingJobsFragment
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.layout_drawer_header.view.*


class HomeActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener, RecyclerItemClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerHeaderView: View
    private lateinit var navigationView: NavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private lateinit var viewBinder: ActivityHomeBinding

    private var pendingJobsFragment: PendingJobsFragment? = null
    private var completedJobsFragment: CompletedJobsFragment? = null

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var fragmentList: List<Fragment>
    private lateinit var fragmentNamesList: List<String>

    companion object {
        private val TAG: String = HomeActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityHomeBinding.inflate(layoutInflater)
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

        viewPager = viewBinder.contentView.viewPager
        tabLayout = viewBinder.contentView.tabLayout

        setupToolbar(toolbar, "Jobs")
        updateDrawerViews()

        val drawerToggle = ActionBarDrawerToggle(this,
            drawerLayout, toolbar,
            R.string.na, R.string.na)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

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

    private fun initFragmentViews() {
        fragmentNamesList = listOf("Pending", "Completed")
        // Initialize fragments
        pendingJobsFragment =
            PendingJobsFragment()
        completedJobsFragment =
            CompletedJobsFragment()
        // Add them to the list
        fragmentList = listOf<Fragment>(pendingJobsFragment!!, completedJobsFragment!!)
        // Initialize adapter
        viewPagerAdapter = ViewPagerAdapter(this, fragmentList)
        // Add adapter to view-pager
        viewPager.adapter = viewPagerAdapter
    }

    private fun setupViewPagerWithTabs() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if(fragmentNamesList[position] == "Pending"){
                tab.icon = PlatformUtils
                    .fetchAttributeDrawable(this, R.attr.iconPendingJobs)
            } else {
                tab.icon = PlatformUtils
                    .fetchAttributeDrawable(this, R.attr.iconCompletedJobs)
            }
            tab.text = fragmentNamesList[position]
        }.attach()
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
        accountIntent.putExtra(IS_ACCOUNT_GRAPH, true)
        fireIntentWithFinish(accountIntent, false)
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

    fun navigateToLoginForReAuth() {
        LogHelper.debug(TAG, "Temporary sign-out complete")
        fireIntentWithFinish(Intent(this, AuthActivity::class.java), false)
    }

    private fun navigateToSettings() {
        val settingsIntent = Intent(this, ViewHostActivity::class.java)
        settingsIntent.putExtra(VIEW_HOST_LAUNCH_GRAPH, viewHostLaunchActionList[1])
        fireIntentWithFinish(settingsIntent, true)
    }

    private fun navigateToJob(workflowID: String) {
        val jobIntent = Intent(this, ViewHostActivity::class.java)
        jobIntent.putExtra(VIEW_HOST_LAUNCH_GRAPH, viewHostLaunchActionList[0])
        jobIntent.putExtra(AppConstants.WORKFLOW_ID_ARG, workflowID)
        fireIntentWithFinish(jobIntent, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingJobsFragment = null
        completedJobsFragment = null
    }

    override fun onItemClick(data: Any) {
        val workflowID = data as String
        navigateToJob(workflowID)
    }
}
