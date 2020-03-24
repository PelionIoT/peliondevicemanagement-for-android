package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.AccountModel
import com.arm.peliondevicemanagement.components.models.LoginHistoryModel
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import kotlinx.android.synthetic.main.layout_item_account.view.*
import kotlinx.android.synthetic.main.layout_item_loginhistory.view.*

class LoginHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private lateinit var loginHistoryModel: LoginHistoryModel

    internal fun bind(model: LoginHistoryModel) {
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