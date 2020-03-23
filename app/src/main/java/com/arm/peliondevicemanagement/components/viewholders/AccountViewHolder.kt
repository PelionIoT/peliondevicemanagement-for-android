package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.AccountModel
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
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
            cardAccountItem.isChecked = SharedPrefHelper
                .getSelectedAccountID() == model.accountID
            tvName.text = model.accountName
        }
    }

}