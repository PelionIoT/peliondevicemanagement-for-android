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
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_COMPLETED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_DISCONNECTED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_FAILED
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_PENDING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import kotlinx.android.synthetic.main.layout_item_device.view.*

class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private lateinit var workflowDeviceModel: WorkflowDeviceModel

    internal fun bind(model: WorkflowDeviceModel) {
        this.workflowDeviceModel = model
        itemView.apply {
            tvName.text = model.deviceName
            tvDescription.text = model.deviceState

            when (model.deviceState) {
                DEVICE_STATE_COMPLETED -> {
                    viewProgressbar.visibility = View.INVISIBLE
                    viewDeviceStatus.visibility = View.VISIBLE
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_ok)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_check_light))
                }
                DEVICE_STATE_FAILED -> {
                    viewProgressbar.visibility = View.INVISIBLE
                    viewDeviceStatus.visibility = View.VISIBLE
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_failed)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
                }
                DEVICE_STATE_PENDING -> {
                    viewProgressbar.visibility = View.INVISIBLE
                    viewDeviceStatus.visibility = View.VISIBLE
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_pending)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
                }
                else -> {
                    viewDeviceStatus.visibility = View.GONE
                    viewProgressbar.visibility = View.VISIBLE
                }
            }
        }
    }

}