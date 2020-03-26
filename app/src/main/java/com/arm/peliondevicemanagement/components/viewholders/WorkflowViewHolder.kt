package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import kotlinx.android.synthetic.main.layout_item_account.view.tvName
import kotlinx.android.synthetic.main.layout_item_workflow.view.*

class WorkflowViewHolder(itemView: View,
                         private val itemClickListener: RecyclerItemClickListener): RecyclerView.ViewHolder(itemView) {

    private lateinit var workflowModel: WorkflowModel

    init {
        itemView.setOnClickListener {
            itemClickListener.onItemClick(workflowModel)
        }
    }

    internal fun bind(model: WorkflowModel) {
        this.workflowModel = model
        itemView.apply {
            tvName.text = model.workflowName
            if(model.workflowAUDs.size == 1){
                chipDeviceCount.text = model.workflowAUDs.size.toString() + " Device"
            } else {
                chipDeviceCount.text = model.workflowAUDs.size.toString() + " Devices"
            }
            chipLocation.text = model.workflowLocation
            if(model.workflowStatus == "PENDING"){
                syncStatusView.setBackgroundColor(resources.getColor(R.color.arm_yellow))
                syncStatusCheckView.background = resources.getDrawable(R.drawable.ic_status_pending)
                syncStatusCheckView.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
            } else {
                syncStatusView.setBackgroundColor(resources.getColor(R.color.arm_green))
                syncStatusCheckView.background = resources.getDrawable(R.drawable.ic_status_ok)
                syncStatusCheckView.setImageDrawable(resources.getDrawable(R.drawable.ic_check_light))
            }
        }
    }

}