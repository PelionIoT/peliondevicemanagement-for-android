package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.LicenseModel
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.data.WorkflowsResponse
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LicensesViewModel : ViewModel() {

    companion object {
        private val TAG: String = LicensesViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val repository: CloudRepository = AppController.getCloudRepoManager()

    val licensesLiveData = MutableLiveData<List<LicenseModel>>()

    fun getLicenses() {
        scope.launch {
            try {
                val licensesResponse = repository.getLicenses()
                licensesLiveData.postValue(licensesResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                licensesLiveData.postValue(null)
            }
        }
    }
    fun cancelAllRequests() = coroutineContext.cancel()

}