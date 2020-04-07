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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.ActivityHostBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.fragments.AccountsFragmentDirections
import com.arm.peliondevicemanagement.screens.fragments.DashboardFragmentDirections
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.layout_drawer_header.view.*

class HostActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationController: NavController
    private lateinit var navigationView: NavigationView

    private lateinit var viewBinder: ActivityHostBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navigationMenu: Menu

    private var isAccountGraph: Boolean = false

    companion object {
        private val TAG: String = HostActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityHostBinding.inflate(layoutInflater)
        initTheme()
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
    }

    private fun initTheme() {
        if(SharedPrefHelper.isDarkThemeEnabled()){
            setAppTheme(true)
        } else {
            setAppTheme(false)
        }
    }

    private fun init() {
        toolbar = viewBinder.toolbar
        drawerLayout = viewBinder.drawerLayout
        navigationController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navigationView = viewBinder.navigationView
        navigationMenu = navigationView.menu

        setupToolbar()

        NavigationUI.setupActionBarWithNavController(this, navigationController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navigationController)

        setupListeners()
    }

    private fun setupListeners() {
        navigationView.setNavigationItemSelectedListener(this)
        navigationController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id){
                R.id.loginFragment -> {
                    showHideToolbar(false)
                    enableDisableDrawer(false)
                }
                R.id.accountsFragment -> {
                    isAccountGraph = true
                    showHideToolbar(true)
                    updateToolbarTitle("Accounts")
                    updateDrawerText(SharedPrefHelper.getUserName())
                    enableDisableDrawer(true)
                    showHideDrawerItems(false)
                }
                R.id.dashboardFragment -> {
                    isAccountGraph = false
                    showHideToolbar(true)
                    updateToolbarTitle("Jobs")
                    updateDrawerText(SharedPrefHelper.getUserName())
                    enableDisableDrawer(true)
                    showHideDrawerItems(true)
                }
                R.id.jobFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Job")
                    enableDisableDrawer(false)
                }
                R.id.jobRunFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Job Run")
                    enableDisableDrawer(false)
                }
                R.id.settingsFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Settings")
                    enableDisableDrawer(false)
                }
                R.id.activityInfoFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Login History")
                    enableDisableDrawer(false)
                }
                R.id.helpAndSupportFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Help & Support")
                    enableDisableDrawer(false)
                }
                R.id.webViewFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("In-App Browsing")
                    enableDisableDrawer(false)
                }
                R.id.licensesFragment -> {
                    showHideToolbar(true)
                    updateToolbarTitle("Libraries we use")
                    enableDisableDrawer(false)
                }
            }
        }
    }

    fun setAppTheme(dark: Boolean) = if(dark){
        setTheme(R.style.AppThemeDark_Launcher)
    } else {
        setTheme(R.style.AppTheme_Launcher)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.accountsFragment,
                R.id.dashboardFragment
            ), viewBinder.drawerLayout
        )
    }

    private fun updateToolbarTitle(toolbarTitle: String) {
        supportActionBar?.apply {
            title = toolbarTitle
        }
    }

    private fun showHideToolbar(visibility: Boolean) = if(visibility) {
        viewBinder.toolbar.visibility = View.VISIBLE
    } else {
        viewBinder.toolbar.visibility = View.GONE
    }

    private fun enableDisableDrawer(enabled: Boolean) = if(enabled) {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    } else {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun showHideDrawerItems(visibility: Boolean){
        if(visibility) {
            navigationMenu.clear()
            navigationView.inflateMenu(R.menu.menu_drawer)
        } else {
            navigationMenu.clear()
            navigationView.inflateMenu(R.menu.menu_drawer)
            if(SharedPrefHelper.isMultiAccountSupported()){
                navigationMenu.findItem(R.id.switch_account).isVisible = false
            }
            navigationMenu.findItem(R.id.settings).isVisible = false
        }
    }

    private fun updateDrawerText(text: String) {
        viewBinder.drawerLayout.tvUserEmail.text = text
        if(!SharedPrefHelper.getSelectedAccountID().isNullOrBlank() &&
                !SharedPrefHelper.getSelectedAccountName().isNullOrBlank()){
            viewBinder.drawerLayout.tvUserAccountName.text =
                SharedPrefHelper.getSelectedAccountName()
            viewBinder.drawerLayout.accountHeader.visibility = View.VISIBLE
        } else {
            viewBinder.drawerLayout.accountHeader.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navigationController, appBarConfiguration)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        drawerLayout.closeDrawers()

        when(item.itemId) {
            R.id.switch_account -> {
                navigationController.navigate(DashboardFragmentDirections.actionDashboardFragmentToAccountsFragment())
            }
            R.id.settings -> {
                navigationController.navigate(R.id.settingsFragment)
            }
            R.id.signout -> {
                WorkflowUtils.deleteWorkflowsCache()
                SharedPrefHelper.storeMultiAccountStatus(false)
                SharedPrefHelper.clearEverything()
                LogHelper.debug(TAG, "Sign-out complete")
                if(isAccountGraph){
                    navigationController.navigate(
                        AccountsFragmentDirections
                            .actionAccountsFragmentToLoginFragment())
                } else {
                    navigationController.navigate(
                        DashboardFragmentDirections
                            .actionDashboardFragmentToLoginFragment())
                }
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
