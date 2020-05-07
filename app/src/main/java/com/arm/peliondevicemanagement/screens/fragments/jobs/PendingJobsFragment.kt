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

package com.arm.peliondevicemanagement.screens.fragments.jobs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.WorkflowAdapter
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.workflow.Workflow
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.constants.state.LoadState
import com.arm.peliondevicemanagement.databinding.FragmentPendingJobsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper

class PendingJobsFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = PendingJobsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentPendingJobsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var workflowViewModel: WorkflowViewModel
    private var workflowAdapter = WorkflowAdapter(this)

    private lateinit var itemClickListener: RecyclerItemClickListener

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
        _viewBinder = FragmentPendingJobsBinding.inflate(inflater, container, false)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        itemClickListener = context as RecyclerItemClickListener
    }

    private fun init() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)

        workflowViewModel.initPendingWorkflowLiveData()

        viewBinder.rvWorkflows.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowAdapter
        }

        @Suppress("DEPRECATION")
        viewBinder.syncProgressView.indeterminateDrawable.setColorFilter(
            resources.getColor(android.R.color.black),
            android.graphics.PorterDuff.Mode.MULTIPLY)

        resetSearchText()
    }

    private fun setupListeners() {
        viewBinder.swipeRefreshLayout.setOnRefreshListener(refreshListener)
        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)

        workflowViewModel.getPendingWorkflows().observe(viewLifecycleOwner, Observer {
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
                    setSwipeRefreshStatus(false)
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
                    showHide404View(true)
                }
            }
        })
    }

    private fun refreshContent() {
        LogHelper.debug(TAG, "refreshContent()")

        showHideSearchBar(false)
        showHide404View(false)
        workflowViewModel.refreshPendingWorkflows()
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

    private fun showHide404View(visibility: Boolean) {
        if (visibility) {
            viewBinder.notFoundView.viewStatus.visibility = View.GONE
            viewBinder.notFoundView.errorText.text = resources.getString(R.string.no_jobs_text)
            viewBinder.notFoundView.root.visibility = View.VISIBLE
        } else {
            viewBinder.notFoundView.root.visibility = View.GONE
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as Workflow
        LogHelper.debug(
            TAG, "onItemClick()-> " +
                "workflowName: ${model.workflowName}, " +
                "workflowID: ${model.workflowID}")
        // Pass it to Home-Activity for launch
        itemClickListener.onItemClick(model.workflowID)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        workflowViewModel.cancelAllRequests()
        _viewBinder = null
    }
}
