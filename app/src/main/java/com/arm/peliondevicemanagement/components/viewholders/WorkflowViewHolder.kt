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

package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import kotlinx.android.synthetic.main.layout_item_account.view.tvName
import kotlinx.android.synthetic.main.layout_item_workflow.view.*

class WorkflowViewHolder(itemView: View,
                         private val itemClickListener: RecyclerItemClickListener): RecyclerView.ViewHolder(itemView) {

    private lateinit var workflowModel: WorkflowModel

    init {
        itemView.setOnClickListener {
            itemClickListener.onItemClick(workflowModel)
        }
    }

    internal fun bind(model: WorkflowModel) {
        this.workflowModel = model
        itemView.apply {
            tvName.text = model.workflowName
            if(model.workflowAUDs.size == 1){
                chipDeviceCount.text = model.workflowAUDs.size.toString() + " Device"
            } else {
                chipDeviceCount.text = model.workflowAUDs.size.toString() + " Devices"
            }
            chipLocation.text = model.workflowLocation
            if(model.workflowStatus == "PENDING"){
                syncStatusView.setBackgroundColor(resources.getColor(R.color.arm_yellow))
                syncStatusCheckView.background = resources.getDrawable(R.drawable.ic_status_pending)
                syncStatusCheckView.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
            } else {
                syncStatusView.setBackgroundColor(resources.getColor(R.color.arm_green))
                syncStatusCheckView.background = resources.getDrawable(R.drawable.ic_status_ok)
                syncStatusCheckView.setImageDrawable(resources.getDrawable(R.drawable.ic_check_light))
            }
        }
    }

}