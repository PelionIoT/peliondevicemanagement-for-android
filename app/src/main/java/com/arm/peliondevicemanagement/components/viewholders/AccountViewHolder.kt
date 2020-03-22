package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.AccountModel
import kotlinx.android.synthetic.main.layout_item_account.view.*

class AccountViewHolder(itemView: View,
                        private val itemClickListener: RecyclerItemClickListener): RecyclerView.ViewHolder(itemView) {

    private lateinit var accountModel: AccountModel

    init {
        itemView.setOnClickListener {
            itemClickListener.onItemClick(accountModel)
        }
    }

    internal fun bind(model: AccountModel) {
        this.accountModel = model
        itemView.apply {
            cardAccountItem.isChecked = model.isSelected
            tvName.text = model.accountName
        }
    }

}