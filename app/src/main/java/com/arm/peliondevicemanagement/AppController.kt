package com.arm.peliondevicemanagement

import android.app.Application
import android.content.res.Configuration
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudAPIService

class AppController : Application() {

    companion object {
        private val TAG: String = AppController::class.java.simpleName
        internal var appController: AppController? = null
        private var cloudRepository: CloudRepository? = null
        private var cloudAPIService: CloudAPIService? = null

        internal fun getCloudRepoManager(): CloudRepository = cloudRepository!!
    }

    override fun onCreate() {
        super.onCreate()
        LogHelper.debug(TAG, "onApplicationCreate()")
        appController = this
        cloudAPIService = CloudAPIService()
        cloudRepository =
            CloudRepository(
                cloudAPIService!!
            )
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