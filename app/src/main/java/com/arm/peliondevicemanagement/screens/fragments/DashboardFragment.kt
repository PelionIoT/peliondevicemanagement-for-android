/*
 * Copyright (c) 2018, Arm Limited and affiliates.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.components.adapters.WorkflowAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.databinding.FragmentDashboardBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.constants.LoadState

class DashboardFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = DashboardFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentDashboardBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var workflowViewModel: WorkflowViewModel
    private var workflowAdapter = WorkflowAdapter(this)

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false

        override fun onQueryTextChange(newText: String?): Boolean {
            //oldWorkflowAdapter!!.filter.filter(newText)
            return false
        }
    }

    private val refreshListener: androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener = androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {
        refreshContent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentDashboardBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
        showHideSearchBar(false)
        showHide404View(false)
        setSwipeRefreshStatus(true)
    }

    private fun init() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)

        viewBinder.rvWorkflows.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowAdapter
        }

        viewBinder.syncProgressView.indeterminateDrawable.setColorFilter(
            resources.getColor(android.R.color.black),
            android.graphics.PorterDuff.Mode.MULTIPLY)

        resetSearchText()
    }

    private fun setupListeners() {
        viewBinder.swipeRefreshLayout.setOnRefreshListener(refreshListener)
        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)

        workflowViewModel.getWorkflows().observe(viewLifecycleOwner, Observer {
            if(it != null && it.isNotEmpty()){
                setSwipeRefreshStatus(false)
            }
            workflowAdapter.submitList(it)
        })

        workflowViewModel.getRefreshState().observe(viewLifecycleOwner, Observer { state->
            when (state) {
                LoadState.LOADING -> {
                    setSwipeRefreshStatus(true)
                }
                LoadState.LOADED -> {
                    setSwipeRefreshStatus(false)
                    updateSyncView(false)
                }
                LoadState.DOWNLOADING -> {
                    updateSyncView(true, "Downloading Assets")
                }
                LoadState.DOWNLOADED -> {
                    updateSyncView(true, "Downloaded successfully")
                }
                LoadState.FAILED -> {
                    updateSyncView(true, "Download failed")
                }
                else -> {
                    updateSyncView(false)
                    setSwipeRefreshStatus(false)
                    showHide404View(true, "No jobs assigned")
                }
            }
        })
    }

    private fun refreshContent() {
        LogHelper.debug(TAG, "refreshContent()")

        showHideSearchBar(false)
        showHide404View(false)
        //updateSyncView(true)
        workflowViewModel.refresh()
    }

    private fun updateSyncView(visibility: Boolean, text: String? = null) {
        if(visibility) {
            viewBinder.syncView.visibility = View.VISIBLE
            if(!text.isNullOrEmpty()){
                viewBinder.syncSubText.visibility = View.VISIBLE
                viewBinder.syncSubText.text = text
            } else {
                viewBinder.syncSubText.visibility = View.GONE
            }
        } else {
            viewBinder.syncView.visibility = View.GONE
        }
    }

    private fun setSwipeRefreshStatus(isRefreshing: Boolean) {
        viewBinder.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun showHideSearchBar(visibility: Boolean) = if(visibility){
        viewBinder.searchBar.root.visibility = View.VISIBLE
    } else {
        viewBinder.searchBar.root.visibility = View.GONE
    }

    private fun resetSearchText() =
        viewBinder.searchBar.searchTextBox.setQuery("", false)

    private fun showHide404View(visibility: Boolean, message: String = "") {
        if (visibility) {
            viewBinder.notFoundView.root.visibility = View.VISIBLE

            if (message.isNotEmpty()) {
                viewBinder.notFoundView.errorText.text = message
            }
        } else {
            viewBinder.notFoundView.root.visibility = View.GONE
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as WorkflowModel
        LogHelper.debug(TAG, "onItemClick()-> " +
                "workflowName: ${model.workflowName}, " +
                "workflowID: ${model.workflowID}")
        navigateToJobFragment(model)
    }

    private fun navigateToJobFragment(workflowModel: WorkflowModel) {
        Navigation.findNavController(viewBinder.root)
            .navigate(DashboardFragmentDirections
                .actionDashboardFragmentToJobFragment(workflowModel))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        workflowViewModel.cancelAllRequests()
    }
}
