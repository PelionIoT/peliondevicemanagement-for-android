package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.ProfileModel
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.data.LoginResponse
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LoginViewModel : ViewModel() {

    companion object {
        private val TAG: String = LoginViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val repository: CloudRepository = AppController.getCloudRepoManager()

    val userAccountLiveData = MutableLiveData<LoginResponse>()
    val userProfileLiveData = MutableLiveData<ProfileModel>()

    fun doLogin(username: String, password: String, accountID: String = "") {
        scope.launch {
            try {
                val userAccountResponse = repository.doAuth(username, password, accountID)
                userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userAccountLiveData.postValue(null)
            }
        }
    }

    fun doImpersonate(accountID: String) {
        scope.launch {
            try {
                val userAccountResponse = repository.doImpersonate(accountID)
                userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userAccountLiveData.postValue(null)
            }
        }
    }

    fun getProfile() {
        scope.launch {
            try {
                val userProfileResponse = repository.getProfile()
                userProfileLiveData.postValue(userProfileResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                userProfileLiveData.postValue(null)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}