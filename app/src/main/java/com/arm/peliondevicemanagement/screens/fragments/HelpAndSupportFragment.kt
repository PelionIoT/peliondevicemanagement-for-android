package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.components.adapters.HelpAndSupportAdapter
import com.arm.peliondevicemanagement.components.models.HelpAndSupportModel
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.databinding.FragmentHelpAndSupportBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A simple [Fragment] subclass.
 */
class HelpAndSupportFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = HelpAndSupportFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentHelpAndSupportBinding? = null
    private val viewBinder get() = _viewBinder!!

    private var helpAndSupportAdapter: HelpAndSupportAdapter? = null
    private var helpAndSupportModelsList = arrayListOf<HelpAndSupportModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentHelpAndSupportBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        init()
    }

    private fun setupData(){
        val fetchedJSON = PlatformUtils.getJsonFromAssets(
            this@HelpAndSupportFragment.requireContext(),
            AppConstants.ABOUT_JSON_FILE)
        val type = object: TypeToken<List<HelpAndSupportModel>>() {}.type
        val supportDataList = Gson().fromJson<ArrayList<HelpAndSupportModel>>(fetchedJSON, type)
        LogHelper.debug(TAG, "helpAndSupportData: $supportDataList")
        helpAndSupportModelsList = supportDataList
    }

    private fun init() {
        helpAndSupportAdapter = HelpAndSupportAdapter(helpAndSupportModelsList, this)
        viewBinder.rvHelpAndSupport.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = helpAndSupportAdapter
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as HelpAndSupportModel
        LogHelper.debug(TAG, "title: ${model.title}, url: ${model.url}")

        if(model.url != "redirect_cuis_licenses")
            navigateToWebViewFragment(model.url)
        else
            navigateToLicensesFragment()
    }

    private fun navigateToWebViewFragment(url: String) {
        Navigation.findNavController(viewBinder.root)
            .navigate(HelpAndSupportFragmentDirections
                .actionHelpAndSupportFragmentToWebViewFragment(url))
    }

    private fun navigateToLicensesFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(HelpAndSupportFragmentDirections
                .actionHelpAndSupportFragmentToLicensesFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }

}
