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

package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_STATE_SYNCED
import com.arm.peliondevicemanagement.constants.state.workflow.WorkflowState
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils
import com.arm.peliondevicemanagement.utils.WorkflowUtils.isValidSDAToken
import kotlinx.android.synthetic.main.layout_item_account.view.tvName
import kotlinx.android.synthetic.main.layout_item_workflow.view.*

class WorkflowViewHolder(itemView: View,
                         private val itemClickListener: RecyclerItemClickListener): RecyclerView.ViewHolder(itemView) {

    private lateinit var workflowModel: Workflow

    init {
        itemView.setOnClickListener {
            itemClickListener.onItemClick(workflowModel)
        }
    }

    internal fun bind(model: Workflow) {
        this.workflowModel = model
        itemView.apply {
            tvName.text = model.workflowName
            val deviceCountText: String
            if(model.workflowAUDs.size == 1){
                deviceCountText = model.workflowAUDs.size.toString() + " Device"
                tvDevices.text = deviceCountText
            } else {
                deviceCountText = model.workflowAUDs.size.toString() + " Devices"
                tvDevices.text = deviceCountText
            }
            tvLocation.text = model.workflowLocation
            // Set workflow-status
            when(model.workflowStatus){
                WorkflowState.COMPLETED.name -> {
                    syncStatusCheckView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_ok)
                    syncStatusCheckView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_light))
                }
                WorkflowState.SYNCED.name -> {
                    if(model.sdaToken != null){
                        if(model.sdaToken!!.isValid){
                            syncStatusCheckView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_synced)
                            syncStatusCheckView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_light))
                        } else {
                            syncStatusCheckView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_failed)
                            syncStatusCheckView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_close_light))
                        }
                    } else {
                        syncStatusCheckView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_pending)
                        syncStatusCheckView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_exclamation))
                    }
                } else -> {
                    syncStatusCheckView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_pending)
                    syncStatusCheckView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_exclamation))
                }
            }
            // Set SDA-Expiry time
            if(model.sdaToken != null){
                if(model.sdaToken!!.isValid){
                    tvDateTime.text = model.sdaToken!!.readableDateTime
                } else {
                    tvDateTime.text = resources.getText(R.string.expired_text)
                }
            } else {
                tvDateTime.text = resources.getString(R.string.na)
            }

        }
    }

}