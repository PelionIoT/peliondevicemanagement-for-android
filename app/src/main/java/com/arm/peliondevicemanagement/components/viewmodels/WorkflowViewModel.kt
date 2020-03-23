package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.ProfileModel
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.CloudRepository
import com.arm.peliondevicemanagement.services.data.LoginResponse
import com.arm.peliondevicemanagement.services.data.WorkflowsResponse
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WorkflowViewModel : ViewModel() {

    companion object {
        private val TAG: String = WorkflowViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val repository: CloudRepository = AppController.getCloudRepoManager()

    val assignedWorkflowsLiveData = MutableLiveData<WorkflowsResponse>()
    val allWorkflowsLiveData = MutableLiveData<WorkflowsResponse>()

    fun getAssignedWorkflows() {
        scope.launch {
            try {
                val workflowsResponse = repository.getAssignedWorkflows()
                assignedWorkflowsLiveData.postValue(workflowsResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                assignedWorkflowsLiveData.postValue(null)
            }
        }
    }

    fun getAllWorkflows() {
        scope.launch {
            try {
                val workflowsResponse = repository.getAllWorkflows()
                allWorkflowsLiveData.postValue(workflowsResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                allWorkflowsLiveData.postValue(null)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}