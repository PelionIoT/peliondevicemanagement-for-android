package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.CloudRepository
import com.arm.peliondevicemanagement.services.data.UserAccountResponse
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LoginViewModel : ViewModel() {

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default

    private val scope = CoroutineScope(coroutineContext)
    private val repository: CloudRepository = AppController.getCloudRepoManager()

    val userAccountLiveData = MutableLiveData<UserAccountResponse>()

    fun doLogin(username: String, password: String) {
        scope.launch {
            val userAccountResponse = repository.doAuth(username, password)
            userAccountLiveData.postValue(userAccountResponse)
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}