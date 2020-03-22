package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentDashboardBinding
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity

class DashboardFragment : Fragment() {

    companion object {
        private val TAG: String = DashboardFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentDashboardBinding? = null
    private val viewBinder get() = _viewBinder!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentDashboardBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as HostActivity).showHideToolbar(true)
        (activity as HostActivity).updateDrawerText(SharedPrefHelper.getUserName())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }
}
