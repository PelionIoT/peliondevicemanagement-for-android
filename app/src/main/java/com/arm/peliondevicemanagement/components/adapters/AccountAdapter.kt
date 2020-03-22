package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.viewholders.AccountViewHolder
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.AccountModel
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import java.util.*

class AccountAdapter(private val accountsList: ArrayList<AccountModel>,
                     private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<AccountViewHolder>(),
    RecyclerItemClickListener,
    Filterable {

    companion object {
        private val TAG: String = AccountAdapter::class.java.simpleName
    }

    private var accountsListFiltered: ArrayList<AccountModel> = accountsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_account,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) =
        holder.bind(model = accountsListFiltered[position])

    override fun getItemCount(): Int = accountsListFiltered.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val searchedText = charSequence.trim().toString().toLowerCase()
            accountsListFiltered = when {
                searchedText.isBlank() -> accountsList
                else -> {
                    val filteredList = arrayListOf<AccountModel>()
                    accountsList
                        .filterTo(filteredList)
                        {
                            it.accountName.toLowerCase(Locale.getDefault()).contains(searchedText)
                        }
                    filteredList
                }
            }
            val filterResults = FilterResults()
            filterResults.values = accountsListFiltered
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            accountsListFiltered = filterResults.values as ArrayList<AccountModel>
            notifyDataSetChanged()
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as AccountModel
        itemClickListener.onItemClick(model)
    }

}