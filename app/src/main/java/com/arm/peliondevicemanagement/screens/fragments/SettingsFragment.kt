package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.databinding.FragmentSettingsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity

class SettingsFragment : Fragment() {

    companion object {
        private val TAG: String = SettingsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentSettingsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val checkedChangeListener: RadioGroup.OnCheckedChangeListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
        when (checkedId) {
            R.id.rbThemeDark -> {
                setThemeAndCallRecreate(true)
            }
            R.id.rbThemeLight -> {
                setThemeAndCallRecreate(false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
    }

    private fun init() {
        LogHelper.debug(TAG, "darkThemeEnabled: ${SharedPrefHelper.isDarkThemeEnabled()}")
        if(SharedPrefHelper.isDarkThemeEnabled()){
            viewBinder.rbThemeDark.isChecked = true
        }
    }

    private fun setupListeners() {
        viewBinder.rgTheme.setOnCheckedChangeListener(checkedChangeListener)

        viewBinder.userActivityButton.setOnClickListener {
            navigateToActivityInfoFragment()
        }

        viewBinder.helpSupportButton.setOnClickListener {
            navigateToHelpAndSupportFragment()
        }
    }

    private fun setThemeAndCallRecreate(isDark: Boolean) {
        if(isDark){
            SharedPrefHelper.setDarkThemeStatus(true)
        } else
            SharedPrefHelper.setDarkThemeStatus(false)

        (activity as HostActivity).setAppTheme(true)
        (activity as HostActivity).recreate()
    }

    private fun navigateToActivityInfoFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToActivityInfoFragment())
    }

    private fun navigateToHelpAndSupportFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToHelpAndSupportFragment())
    }
}
