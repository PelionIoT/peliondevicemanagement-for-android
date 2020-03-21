package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.findNavController

import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentAccountsBinding
import com.arm.peliondevicemanagement.databinding.FragmentLoginBinding
import com.arm.peliondevicemanagement.screens.activities.HostActivity

class AccountsFragment : Fragment() {

    private var _viewBinder: FragmentAccountsBinding? = null
    private val viewBinder get() = _viewBinder!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentAccountsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as HostActivity).showHideToolbar(true)

        viewBinder.testButton.setOnClickListener {
            Navigation.findNavController(viewBinder.root).navigate(AccountsFragmentDirections.actionAccountsFragmentToDashboardFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
