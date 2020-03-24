package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.AccountModel
import com.arm.peliondevicemanagement.components.models.HelpAndSupportModel
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import kotlinx.android.synthetic.main.layout_item_account.view.*

class HelpAndSupportViewHolder(itemView: View,
                               private val itemClickListener: RecyclerItemClickListener): RecyclerView.ViewHolder(itemView) {

    private lateinit var helpAndSupportModel: HelpAndSupportModel

    init {
        itemView.setOnClickListener {
            itemClickListener.onItemClick(helpAndSupportModel)
        }
    }

    internal fun bind(model: HelpAndSupportModel) {
        this.helpAndSupportModel = model
        itemView.apply {
            tvName.text = model.title
        }
    }

}