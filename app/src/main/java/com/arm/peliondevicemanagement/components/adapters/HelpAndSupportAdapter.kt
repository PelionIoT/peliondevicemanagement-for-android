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
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.HelpAndSupportModel
import com.arm.peliondevicemanagement.components.viewholders.HelpAndSupportViewHolder
import java.util.*

class HelpAndSupportAdapter(private val helpAndSupportList: ArrayList<HelpAndSupportModel>,
                            private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<HelpAndSupportViewHolder>(),
    RecyclerItemClickListener {

    companion object {
        private val TAG: String = HelpAndSupportAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpAndSupportViewHolder {
        return HelpAndSupportViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_helpandsupport,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: HelpAndSupportViewHolder, position: Int) =
        holder.bind(model = helpAndSupportList[position])

    override fun getItemCount(): Int = helpAndSupportList.size

    override fun onItemClick(data: Any) {
        itemClickListener.onItemClick(data)
    }

}