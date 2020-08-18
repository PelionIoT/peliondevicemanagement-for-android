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

package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.store.EnrollingIoTDevicesDataSource
import com.arm.peliondevicemanagement.utils.PlatformUtils
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class EnrollingIoTDevicesViewModel : ViewModel() {

    companion object {
        private val TAG: String = EnrollingIoTDevicesViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)

    private val cloudRepository: CloudRepository = AppController.getCloudRepository()

    private lateinit var _devicesLiveData: LiveData<PagedList<EnrollingIoTDevice>>
    private var _enrollingDeviceLiveData = MutableLiveData<EnrollingIoTDevice>()
    private val _refreshStateLiveData = MutableLiveData<LoadState>()
    private val _errorResponseLiveData = MutableLiveData<ErrorResponse>()

    private val boundaryCallback = object: PagedList.BoundaryCallback<EnrollingIoTDevice>() {
        override fun onZeroItemsLoaded() {
            super.onZeroItemsLoaded()
            // Handle empty initial load here
            LogHelper.debug(TAG, "BoundaryCallback()->onZeroItemsLoaded")
            _refreshStateLiveData.postValue(LoadState.EMPTY)
        }

        override fun onItemAtEndLoaded(itemAtEnd: EnrollingIoTDevice) {
            super.onItemAtEndLoaded(itemAtEnd)
            // Here you can listen to last item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtEndLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }

        override fun onItemAtFrontLoaded(itemAtFront: EnrollingIoTDevice) {
            super.onItemAtFrontLoaded(itemAtFront)
            // Here you can listen to first item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtFrontLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }
    }

    fun initEnrollingIoTDevicesLiveData() {
        _devicesLiveData = getEnrollingIoTDevicesLiveData()
    }

    fun getEnrollingIoTDevices(): LiveData<PagedList<EnrollingIoTDevice>> = _devicesLiveData

    fun enrollDevice(identity: String) {
        scope.launch {
            try {
                val enrollingDeviceResponse = cloudRepository.enrollDevice(identity)
                _enrollingDeviceLiveData.postValue(enrollingDeviceResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = PlatformUtils.parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun getRefreshState(): LiveData<LoadState> = _refreshStateLiveData
    fun getEnrollmentStatusLiveData(): LiveData<EnrollingIoTDevice> = _enrollingDeviceLiveData
    fun getErrorResponseLiveData(): LiveData<ErrorResponse> = _errorResponseLiveData

    fun refreshEnrollingDevices() {
        LogHelper.debug(TAG, "Invalidate data using network")
        _devicesLiveData.value?.dataSource?.invalidate()
    }

    private fun getEnrollingIoTDevicesLiveData(): LiveData<PagedList<EnrollingIoTDevice>> {
        val pageConfig = PagedList.Config.Builder()
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setEnablePlaceholders(false)
            .build()

        val dataSourceFactory = buildEnrollingDevicesFactory()

        return LivePagedListBuilder(dataSourceFactory, pageConfig)
            .setBoundaryCallback(boundaryCallback)
            .build()
    }

    private fun buildEnrollingDevicesFactory(): DataSource.Factory<String, EnrollingIoTDevice> {
        return object : DataSource.Factory<String, EnrollingIoTDevice>() {
            override fun create(): DataSource<String, EnrollingIoTDevice> {
                return EnrollingIoTDevicesDataSource(scope, cloudRepository, _refreshStateLiveData)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}