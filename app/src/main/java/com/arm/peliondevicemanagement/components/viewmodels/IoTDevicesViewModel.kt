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
import com.arm.peliondevicemanagement.components.models.devices.IoTDevice
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.store.IoTDevicesDataSource
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class IoTDevicesViewModel : ViewModel() {

    companion object {
        private val TAG: String = IoTDevicesViewModel::class.java.simpleName
        private const val QUERY_DEBOUNCE = 500L
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)

    private val cloudRepository: CloudRepository = AppController.getCloudRepository()

    private lateinit var _devicesLiveData: LiveData<PagedList<IoTDevice>>
    private val _refreshStateLiveData = MutableLiveData<LoadState>()

    val devicesListSearchIndex = arrayListOf<IoTDevice>()

    private val boundaryCallback = object: PagedList.BoundaryCallback<IoTDevice>() {
        override fun onZeroItemsLoaded() {
            super.onZeroItemsLoaded()
            // Handle empty initial load here
            LogHelper.debug(TAG, "BoundaryCallback()->onZeroItemsLoaded")
            _refreshStateLiveData.postValue(LoadState.EMPTY)
        }

        override fun onItemAtEndLoaded(itemAtEnd: IoTDevice) {
            super.onItemAtEndLoaded(itemAtEnd)
            // Here you can listen to last item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtEndLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }

        override fun onItemAtFrontLoaded(itemAtFront: IoTDevice) {
            super.onItemAtFrontLoaded(itemAtFront)
            // Here you can listen to first item on list
            LogHelper.debug(TAG, "BoundaryCallback()->onItemAtFrontLoaded")
            _refreshStateLiveData.postValue(LoadState.LOADED)
        }
    }

    fun initIoTDevicesLiveData() {
        _devicesLiveData = getIoTDevicesLiveData()
    }

    fun getIoTDevices(): LiveData<PagedList<IoTDevice>> = _devicesLiveData
    fun getRefreshState(): LiveData<LoadState> = _refreshStateLiveData

    fun refreshDevices() {
        LogHelper.debug(TAG, "Invalidate data using network")
        _devicesLiveData.value?.dataSource?.invalidate()
    }

    private fun getIoTDevicesLiveData(): LiveData<PagedList<IoTDevice>> {
        val pageConfig = PagedList.Config.Builder()
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setEnablePlaceholders(false)
            .build()

        val dataSourceFactory = buildDevicesFactory()

        return LivePagedListBuilder(dataSourceFactory, pageConfig)
            .setBoundaryCallback(boundaryCallback)
            .build()
    }

    private fun buildDevicesFactory(): DataSource.Factory<String, IoTDevice> {
        return object : DataSource.Factory<String, IoTDevice>() {
            override fun create(): DataSource<String, IoTDevice> {
                return IoTDevicesDataSource(scope, cloudRepository, _refreshStateLiveData, devicesListSearchIndex)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}