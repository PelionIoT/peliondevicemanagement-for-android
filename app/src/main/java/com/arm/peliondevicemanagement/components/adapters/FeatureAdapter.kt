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
import com.arm.peliondevicemanagement.components.viewholders.FeatureViewHolder
import java.util.*

class FeatureAdapter(private val featureList: Array<String>,
                     private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<FeatureViewHolder>(),
    RecyclerItemClickListener {

    companion object {
        private val TAG: String = FeatureAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        return FeatureViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_feature,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) =
        holder.bind(featureName = featureList[position])

    override fun getItemCount(): Int = featureList.size

    override fun onItemClick(data: Any) {
        itemClickListener.onItemClick(data)
    }

}