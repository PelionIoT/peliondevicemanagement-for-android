package com.arm.peliondevicemanagement.screens.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper

open class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = BaseActivity::class.java.simpleName
    }

    private var activeToast: Toast? = null
    private var initiatedCloseMs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogHelper.debug(tag = TAG, msg = "onCreate")
    }

    protected fun setupToolbar(toolbar: Toolbar, title: String?, iconResId: Int?){
        /*if (!SharedPrefHelper.isDarkThemeEnabled()) {
            //toolbar.context.setTheme(R.style.ToolBarThemeLight)
            //toolbar.tvTitleToolbar?.setTextColor(ContextCompat.getColor(this, R.color.text_color_white_for_light))
        } else {
            //toolbar.context.setTheme(R.style.ToolBarThemeDark)
            //toolbar.tvTitleToolbar?.setTextColor(ContextCompat.getColor(this, R.color.text_color_white))
        }

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (iconResId != null)
            supportActionBar!!.setHomeAsUpIndicator(iconResId)
        else {
            when (SharedPrefHelper.isDarkThemeEnabled()) {
                true -> supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back_white)
                false -> supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back_dark)
            }
        }

        supportActionBar!!.title = null*/
    }

    internal fun isAccountNotSelected(): Boolean =
        SharedPrefHelper.getSelectedAccountID().isNullOrBlank()

    internal fun getAppVersion(): String = getString(R.string.version_format, getAppVersionCode())

    private fun getAppVersionCode(): String {
        return if(BuildConfig.DEBUG){
            BuildConfig.VERSION_NAME
        } else {
            var versionName = BuildConfig.VERSION_NAME
            versionName = versionName.substringBeforeLast(".")
            versionName
        }
    }

    internal fun displayToast(message: String){
        // Cancel previous toast
        if(activeToast != null) {
            activeToast!!.cancel()
        }

        // Display new one
        activeToast = Toast.makeText(baseContext, message, Toast.LENGTH_SHORT)
        activeToast!!.show()
    }

    internal fun fireIntent(intent: Intent, isForward: Boolean) {
        startActivity(intent)
        if (isForward)
            overridePendingTransition(R.anim.right_in, R.anim.right_out)
        else
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
    }

    internal fun fireIntentWithFinish(intent: Intent, isForward: Boolean) {
        startActivity(intent)
        if (isForward)
            overridePendingTransition(R.anim.right_in, R.anim.right_out)
        else
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        finish()
    }

    internal fun addFragment(containerId: Int, fragment: Fragment, isForward: Boolean) {
        supportFragmentManager.beginTransaction()
            .apply {
                when (isForward) {
                    true -> setCustomAnimations(R.anim.right_in, R.anim.right_out)
                    false -> setCustomAnimations(R.anim.left_in, R.anim.left_out)
                }
                add(containerId, fragment, fragment.javaClass.simpleName)
            }
            .commitAllowingStateLoss()
    }

    private fun removeFragment(fragment: Fragment, isForward: Boolean) {
        supportFragmentManager.beginTransaction()
            .apply {
                when (isForward) {
                    true -> setCustomAnimations(R.anim.right_in, R.anim.right_out)
                    false -> setCustomAnimations(R.anim.left_in, R.anim.left_out)
                }
                remove(fragment)
            }
            .commitAllowingStateLoss()
    }

    internal fun replaceFragment(containerId: Int, fragment: Fragment, isForward: Boolean) {
        supportFragmentManager.beginTransaction()
            .apply {
                when (isForward) {
                    true -> setCustomAnimations(R.anim.right_in, R.anim.right_out)
                    false -> setCustomAnimations(R.anim.left_in, R.anim.left_out)
                }
                replace(containerId, fragment, fragment.javaClass.simpleName)
            }
            .commitAllowingStateLoss()
    }

    internal fun removeAndAddFragment(containerId: Int, fragmentAdd: Fragment, fragmentRemove: Fragment, isForward: Boolean) {
        supportFragmentManager.beginTransaction()
            .apply {
                when (isForward) {
                    true -> setCustomAnimations(R.anim.right_in, R.anim.right_out)
                    false -> setCustomAnimations(R.anim.left_in, R.anim.left_out)
                }
                remove(fragmentRemove)
                add(containerId, fragmentAdd, fragmentAdd.javaClass.simpleName)
            }
            .commitAllowingStateLoss()
    }

    internal fun callNewLoginWithFinish() {
        SharedPrefHelper.clearUserData(removeCredentials = true, removeAccountId = true)
        fireIntentWithFinish(
            intent = Intent(this@BaseActivity, LaunchActivity::class.java),
            isForward = false)
    }

    protected fun callCloseApp() = if(System.currentTimeMillis() - initiatedCloseMs < 2000){
        finish()
    } else {
        initiatedCloseMs = System.currentTimeMillis()
        displayToast("Press again to exit the app")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        callCloseApp()
    }

}
