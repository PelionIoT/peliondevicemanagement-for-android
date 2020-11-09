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

package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.components.viewholders.EnrollingIoTDeviceViewHolder

class EnrollingIoTDeviceAdapter():
    PagedListAdapter<EnrollingIoTDevice, EnrollingIoTDeviceViewHolder>(ENROLLING_IOT_DEVICE_COMPARATOR){

    companion object {
        private val ENROLLING_IOT_DEVICE_COMPARATOR = object : DiffUtil.ItemCallback<EnrollingIoTDevice>() {
            override fun areItemsTheSame(oldItem: EnrollingIoTDevice, newItem: EnrollingIoTDevice): Boolean =
                oldItem.deviceID == newItem.deviceID

            override fun areContentsTheSame(oldItem: EnrollingIoTDevice, newItem: EnrollingIoTDevice): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnrollingIoTDeviceViewHolder {
        return EnrollingIoTDeviceViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_iot_device, parent, false))
    }

    override fun onBindViewHolder(holder: EnrollingIoTDeviceViewHolder, position: Int){
        getItem(position)?.let { holder.bind(it) }
    }

}