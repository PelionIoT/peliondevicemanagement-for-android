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
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.devices.IoTDevice
import com.arm.peliondevicemanagement.components.viewholders.IoTDeviceViewHolder
import com.arm.peliondevicemanagement.constants.state.devices.DevicesFilters
import java.util.*

class DevicesSearchAdapter(private val devicesList: ArrayList<IoTDevice>,
                           private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<IoTDeviceViewHolder>(),
    RecyclerItemClickListener,
    Filterable {

    companion object {
        private val TAG: String = DevicesSearchAdapter::class.java.simpleName
    }

    private var devicesListFiltered: ArrayList<IoTDevice> = devicesList
    private var activeFilter: DevicesFilters = DevicesFilters.ENDPOINT_NAME

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IoTDeviceViewHolder {
        return IoTDeviceViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_iot_device, parent, false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: IoTDeviceViewHolder, position: Int) =
        holder.bind(model = devicesListFiltered[position])

    override fun getItemCount(): Int = devicesListFiltered.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val searchedText = charSequence.trim().toString().toLowerCase()
            devicesListFiltered = when {
                searchedText.isBlank() -> devicesList
                else -> {
                    val filteredList = arrayListOf<IoTDevice>()
                    devicesList
                        .filterTo(filteredList)
                        {
                            when(activeFilter){
                                DevicesFilters.DEVICE_ID -> {
                                    it.deviceID.toLowerCase(Locale.getDefault()).contains(searchedText)
                                }
                                DevicesFilters.DEVICE_NAME -> {
                                    it.deviceName.toLowerCase(Locale.getDefault()).contains(searchedText)
                                }
                                DevicesFilters.ENDPOINT_NAME -> {
                                    it.endpointName.toLowerCase(Locale.getDefault()).contains(searchedText)
                                }
                            }
                        }
                    filteredList
                }
            }
            val filterResults = FilterResults()
            filterResults.values = devicesListFiltered
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            devicesListFiltered = filterResults.values as ArrayList<IoTDevice>
            notifyDataSetChanged()
        }
    }

    fun setFilterType(filters: DevicesFilters) {
        activeFilter = filters
    }

    override fun onItemClick(data: Any) {
        itemClickListener.onItemClick(data)
    }

}