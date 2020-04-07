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
import com.arm.peliondevicemanagement.components.models.user.UserLoginHistory
import kotlinx.android.synthetic.main.layout_item_loginhistory.view.*

class LoginHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private lateinit var loginHistoryModel: UserLoginHistory

    internal fun bind(model: UserLoginHistory) {
        this.loginHistoryModel = model

        val loginStatus: String = if(model.status)
            "Success"
        else
            "Failed"

        itemView.apply {
            tvIPAddress.text = context.getString(R.string.ip_format, model.ipAddress)
            tvDate.text = context.getString(R.string.date_format, model.date)
            tvStatus.text = context.getString(R.string.status_format, loginStatus)
            tvUserAgent.text = context.getString(R.string.user_agent_format, model.userAgent)
        }
    }

}