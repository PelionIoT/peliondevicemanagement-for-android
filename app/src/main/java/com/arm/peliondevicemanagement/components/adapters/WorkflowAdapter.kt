/*
 * Copyright (c) 2018, Arm Limited and affiliates.
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
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewholders.WorkflowViewHolder

class WorkflowAdapter(private val itemClickListener: RecyclerItemClickListener):
    PagedListAdapter<WorkflowModel, WorkflowViewHolder>(WORKFLOW_COMPARATOR){

    companion object {
        private val WORKFLOW_COMPARATOR = object : DiffUtil.ItemCallback<WorkflowModel>() {
            override fun areItemsTheSame(oldItem: WorkflowModel, newItem: WorkflowModel): Boolean =
                oldItem.workflowID == newItem.workflowID

            override fun areContentsTheSame(oldItem: WorkflowModel, newItem: WorkflowModel): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkflowViewHolder {
        return WorkflowViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_workflow, parent, false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: WorkflowViewHolder, position: Int){
        getItem(position)?.let { holder.bind(it) }
    }

}