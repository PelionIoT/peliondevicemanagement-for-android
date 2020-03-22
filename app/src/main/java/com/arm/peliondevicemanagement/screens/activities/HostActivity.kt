package com.arm.peliondevicemanagement.screens.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.SharedPreferencesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.ActivityHostBinding
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.fragments.DashboardFragmentDirections
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityHostBinding.inflate(layoutInflater)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        init()
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
                    enableDisableDrawer(false)
                }
                R.id.accountsFragment -> {
                    updateToolbarTitle("Accounts")
                    enableDisableDrawer(true)
                    showHideDrawerItems(false)
                }
                R.id.dashboardFragment -> {
                    updateToolbarTitle("Dashboard")
                    enableDisableDrawer(true)
                    showHideDrawerItems(true)
                }
                R.id.settingsFragment -> {
                    updateToolbarTitle("Settings")
                    enableDisableDrawer(false)
                }
            }
        }
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

    fun showHideToolbar(visibility: Boolean) = if(visibility) {
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

    fun updateDrawerText(text: String) {
        viewBinder.drawerLayout.tvUserEmail.text = text
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
                SharedPrefHelper.storeMultiAccountStatus(false)
                SharedPrefHelper.clearEverything()
                navigationController.navigate(R.id.loginFragment)
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
