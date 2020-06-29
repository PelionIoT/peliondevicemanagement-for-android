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

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants.IS_ACCOUNT_GRAPH
import com.arm.peliondevicemanagement.databinding.ActivityAuthBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.fragments.auth.AccountsFragmentDirections
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.layout_drawer_header.view.*

class AuthActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerHeaderView: View
    private lateinit var navigationController: NavController
    private lateinit var navigationView: NavigationView

    private lateinit var viewBinder: ActivityAuthBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var isAccountGraph: Boolean = false

    companion object {
        private val TAG: String = AuthActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityAuthBinding.inflate(layoutInflater)
        initTheme(true)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
    }

    private fun init() {
        // Check if requested action is present in bundle
        isAccountGraph = intent.getBooleanExtra(IS_ACCOUNT_GRAPH, false)
        // Now setup-views
        toolbar = viewBinder.toolbar
        drawerLayout = viewBinder.drawerLayout
        drawerHeaderView = viewBinder.navigationView
            .inflateHeaderView(R.layout.layout_drawer_header)
        navigationController = Navigation
            .findNavController(this, R.id.nav_host_fragment)
        val navGraph = navigationController.navInflater.inflate(R.navigation.auth_nav_graph)
        when(isAccountGraph) {
            true -> {
                navGraph.startDestination = R.id.accountsFragment
            } else -> {
                navGraph.startDestination =R.id.loginFragment
            }
        }
        navigationController.graph = navGraph
        navigationView = viewBinder.navigationView

        setupToolbar()

        NavigationUI.setupActionBarWithNavController(this, navigationController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navigationController)
        setupItemNavViewColors()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.accountsFragment), viewBinder.drawerLayout
        )
    }

    private fun setupListeners() {
        navigationView.setNavigationItemSelectedListener(this)
        navigationController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.loginFragment -> {
                    setToolbarVisibility(false)
                    setDrawerVisibility(false)
                }
                R.id.twoFactorAuthFragment -> {
                    setToolbarVisibility(false)
                    setDrawerVisibility(false)
                }
                R.id.accountsFragment -> {
                    setDrawerVisibility(true)
                    setToolbarVisibility(true)
                    updateDrawerViews()
                }
            }
        }
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

    private fun setToolbarVisibility(visibility: Boolean) = if(visibility) {
        viewBinder.toolbar.visibility = View.VISIBLE
        viewBinder.toolbar.title = "Accounts"
    } else {
        viewBinder.toolbar.visibility = View.GONE
    }

    private fun setDrawerVisibility(enabled: Boolean) = if(enabled) {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    } else {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
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

    fun launchHomeActivity() {
        fireIntentWithFinish(Intent(this, HomeActivity::class.java), true)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navigationController, appBarConfiguration)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        drawerLayout.closeDrawers()

        when(item.itemId) {
            R.id.signout -> {
                navigateToLogin()
            }
        }
        return true
    }

    fun navigateToLogin() {
        WorkflowUtils.deleteWorkflowsCache()
        SharedPrefHelper.storeMultiAccountStatus(false)
        SharedPrefHelper.clearEverything()
        LogHelper.debug(TAG, "Sign-out complete")
        navigationController.navigate(
            AccountsFragmentDirections
                .actionAccountsFragmentToLoginFragment())
    }

}
