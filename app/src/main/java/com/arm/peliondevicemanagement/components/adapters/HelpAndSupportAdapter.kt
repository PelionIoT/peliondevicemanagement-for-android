package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.HelpAndSupportModel
import com.arm.peliondevicemanagement.components.viewholders.HelpAndSupportViewHolder
import com.arm.peliondevicemanagement.helpers.LogHelper
import java.util.*

class HelpAndSupportAdapter(private val helpAndSupportList: ArrayList<HelpAndSupportModel>,
                            private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<HelpAndSupportViewHolder>(),
    RecyclerItemClickListener {

    companion object {
        private val TAG: String = HelpAndSupportAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpAndSupportViewHolder {
        return HelpAndSupportViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_helpandsupport,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: HelpAndSupportViewHolder, position: Int) =
        holder.bind(model = helpAndSupportList[position])

    override fun getItemCount(): Int = helpAndSupportList.size

    override fun onItemClick(data: Any) {
        itemClickListener.onItemClick(data)
    }

}