package com.arm.peliondevicemanagement.screens.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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

    internal fun callNewLoginWithFinish() {
        //SharedPrefHelper.clearUserData(removeCredentials = true, removeAccountId = true)
        fireIntentWithFinish(
            intent = Intent(this@BaseActivity, HostActivity::class.java),
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
