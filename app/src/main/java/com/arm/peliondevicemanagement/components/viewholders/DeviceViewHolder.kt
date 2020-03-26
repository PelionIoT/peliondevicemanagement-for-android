package com.arm.peliondevicemanagement.components.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowDeviceModel
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_CONNECTING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_PENDING
import com.arm.peliondevicemanagement.constants.AppConstants.DEVICE_STATE_RUNNING
import kotlinx.android.synthetic.main.layout_item_device.view.*

class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private lateinit var workflowDeviceModel: WorkflowDeviceModel

    internal fun bind(model: WorkflowDeviceModel) {
        this.workflowDeviceModel = model
        itemView.apply {
            tvName.text = model.deviceName
            tvDescription.text = model.deviceState

            when (model.deviceState) {
                DEVICE_STATE_CONNECTING -> {
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_pending)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
                }
                DEVICE_STATE_RUNNING -> {
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_pending)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
                }
                DEVICE_STATE_PENDING -> {
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_pending)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_exclamation))
                }
                else -> {
                    viewDeviceStatus.background = resources.getDrawable(R.drawable.ic_status_ok)
                    viewDeviceStatus.setImageDrawable(resources.getDrawable(R.drawable.ic_check_light))
                }
            }
        }
    }

}