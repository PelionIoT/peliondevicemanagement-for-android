package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.LoginHistoryModel
import com.arm.peliondevicemanagement.components.viewholders.LoginHistoryViewHolder
import java.util.*

class LoginHistoryAdapter(private val loginHistoryList: ArrayList<LoginHistoryModel>):
    RecyclerView.Adapter<LoginHistoryViewHolder>() {

    companion object {
        private val TAG: String = LoginHistoryAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginHistoryViewHolder {
        return LoginHistoryViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_loginhistory,
                parent,
                false))
    }

    override fun onBindViewHolder(holder: LoginHistoryViewHolder, position: Int) =
        holder.bind(model = loginHistoryList[position])

    override fun getItemCount(): Int = loginHistoryList.size

}