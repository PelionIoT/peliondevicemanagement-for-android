package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentLicensesBinding

/**
 * A simple [Fragment] subclass.
 */
class LicensesFragment : Fragment() {

    companion object {
        private val TAG: String = LicensesFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentLicensesBinding? = null
    private val viewBinder get() = _viewBinder!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentLicensesBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        viewBinder.notFoundView.errorText.text = "Under construction"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
