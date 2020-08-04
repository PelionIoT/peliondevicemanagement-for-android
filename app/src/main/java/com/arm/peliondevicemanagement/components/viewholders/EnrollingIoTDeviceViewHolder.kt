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
import com.arm.peliondevicemanagement.components.models.devices.EnrollingIoTDevice
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.utils.PlatformUtils.fetchAttributeDrawable
import kotlinx.android.synthetic.main.layout_item_iot_device.view.*
import kotlinx.android.synthetic.main.layout_item_iot_device.view.iconView
import kotlinx.android.synthetic.main.layout_item_iot_device.view.tvName

class EnrollingIoTDeviceViewHolder(itemView: View):
    RecyclerView.ViewHolder(itemView) {

    internal fun bind(model: EnrollingIoTDevice) {
        itemView.apply {
            tvName.text = model.enrollmentIdentity
            iconView.setImageDrawable(fetchAttributeDrawable(context, R.attr.iconIoTDevice))

            if(model.claimedAt != null){
                deviceStatusView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_ok)
                deviceStatusView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_light))
            } else {
                deviceStatusView.background = ContextCompat.getDrawable(context, R.drawable.ic_status_pending)
                deviceStatusView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_exclamation))
            }

            tvState.text = model.enrolledDeviceID ?: "Pending"
        }.isClickable = false
    }

}