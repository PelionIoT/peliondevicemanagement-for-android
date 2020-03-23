package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.viewholders.AccountViewHolder
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.AccountModel
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewholders.WorkflowViewHolder
import java.util.*

class WorkflowAdapter(private val workflowList: ArrayList<WorkflowModel>,
                      private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<WorkflowViewHolder>(),
    RecyclerItemClickListener,
    Filterable {

    companion object {
        private val TAG: String = WorkflowAdapter::class.java.simpleName
    }

    private var workflowListFiltered: ArrayList<WorkflowModel> = workflowList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkflowViewHolder {
        return WorkflowViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_workflow,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: WorkflowViewHolder, position: Int) =
        holder.bind(model = workflowListFiltered[position])

    override fun getItemCount(): Int = workflowListFiltered.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val searchedText = charSequence.trim().toString().toLowerCase()
            workflowListFiltered = when {
                searchedText.isBlank() -> workflowList
                else -> {
                    val filteredList = arrayListOf<WorkflowModel>()
                    workflowList
                        .filterTo(filteredList)
                        {
                            it.workflowName.toLowerCase(Locale.getDefault()).contains(searchedText)
                        }
                    filteredList
                }
            }
            val filterResults = FilterResults()
            filterResults.values = workflowListFiltered
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            workflowListFiltered = filterResults.values as ArrayList<WorkflowModel>
            notifyDataSetChanged()
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as WorkflowModel
        itemClickListener.onItemClick(model)
    }

}