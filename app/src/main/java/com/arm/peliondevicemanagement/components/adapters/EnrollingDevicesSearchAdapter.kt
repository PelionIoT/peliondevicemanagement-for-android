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
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.components.viewholders.EnrollingIoTDeviceViewHolder
import java.util.*

class EnrollingDevicesSearchAdapter(private val enrollingDevicesList: ArrayList<EnrollingIoTDevice>):
    RecyclerView.Adapter<EnrollingIoTDeviceViewHolder>(),
    Filterable {

    companion object {
        private val TAG: String = EnrollingDevicesSearchAdapter::class.java.simpleName
    }

    private var enrollingDevicesListFiltered: ArrayList<EnrollingIoTDevice> = enrollingDevicesList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnrollingIoTDeviceViewHolder {
        return EnrollingIoTDeviceViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_iot_device, parent, false))
    }

    override fun onBindViewHolder(holder: EnrollingIoTDeviceViewHolder, position: Int) =
        holder.bind(model = enrollingDevicesListFiltered[position])

    override fun getItemCount(): Int = enrollingDevicesListFiltered.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val searchedText = charSequence.trim().toString().toLowerCase()
            enrollingDevicesListFiltered = when {
                searchedText.isBlank() -> enrollingDevicesList
                else -> {
                    val filteredList = arrayListOf<EnrollingIoTDevice>()
                    enrollingDevicesList
                        .filterTo(filteredList)
                        {
                            it.enrollmentIdentity.toLowerCase(Locale.getDefault()).contains(searchedText)
                        }
                    filteredList
                }
            }
            val filterResults = FilterResults()
            filterResults.values = enrollingDevicesListFiltered
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            enrollingDevicesListFiltered = filterResults.values as ArrayList<EnrollingIoTDevice>
            notifyDataSetChanged()
        }
    }

}