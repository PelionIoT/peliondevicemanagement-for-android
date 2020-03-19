package com.arm.peliondevicemanagement

import android.app.Application
import android.content.res.Configuration
import com.arm.peliondevicemanagement.helpers.LogHelper

class AppController : Application() {

    companion object {
        private val TAG: String = AppController::class.java.simpleName

        internal var appController: AppController? = null
    }

    override fun onCreate() {
        super.onCreate()
        LogHelper.debug(TAG, "onApplicationCreate()")
        appController = this
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LogHelper.debug(TAG, "onConfigurationChanged()")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LogHelper.debug(TAG, "onLowMemory()")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        LogHelper.debug(TAG, "onTrimMemory()")
    }

    override fun onTerminate() {
        super.onTerminate()
        LogHelper.debug(TAG, "onTerminate()")
    }
}