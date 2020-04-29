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

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.setPadding
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
import com.arm.peliondevicemanagement.constants.state.NetworkErrorState
import com.arm.peliondevicemanagement.databinding.FragmentCompletedJobsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.screens.activities.HomeActivity
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.android.material.bottomsheet.BottomSheetDialog

class CompletedJobsFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = CompletedJobsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentCompletedJobsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var workflowViewModel: WorkflowViewModel
    private var workflowAdapter = WorkflowAdapter(this)

    private lateinit var itemClickListener: RecyclerItemClickListener

    private var totalItemsForUpload: Int = 0

    private lateinit var errorBottomSheetDialog: BottomSheetDialog
    private lateinit var retryButtonClickListener: View.OnClickListener

    private val refreshListener: androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener = androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {
        refreshContent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentCompletedJobsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
        showHide404View(false)
        setSwipeRefreshStatus(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        itemClickListener = context as RecyclerItemClickListener
    }

    private fun init() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)

        workflowViewModel.initCompletedWorkflowsLiveData()

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
    }

    private fun setupListeners() {
        viewBinder.swipeRefreshLayout.setOnRefreshListener(refreshListener)

        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog.dismiss()
            prepareForUpload()
        }

        workflowViewModel.getCompletedWorkflows().observe(viewLifecycleOwner, Observer {
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
                    checkJobsPendingForSync()
                }
                else -> {
                    updateSyncView(false)
                    setSwipeRefreshStatus(false)
                    showHide404View(true)
                }
            }
        })

        viewBinder.uploadJobsButton.setOnClickListener {
            prepareForUpload()
        }
    }

    private fun refreshContent() {
        LogHelper.debug(TAG, "refreshContent()")

        showHide404View(false)
        workflowViewModel.refreshCompletedWorkflows()
    }

    private fun checkJobsPendingForSync() {
        totalItemsForUpload = 0
        workflowAdapter.currentList?.forEach { workflow ->
            if(!workflow.uploadCompleted){
                totalItemsForUpload++
            }
        }
        when {
            totalItemsForUpload == 1 -> {
                updateSyncView(true,
                    resources.getString(R.string.sync_pending_text),
                    "$totalItemsForUpload job is waiting in the queue")
            }
            totalItemsForUpload > 1 -> {
                updateSyncView(true,
                    resources.getString(R.string.sync_pending_text),
                    "$totalItemsForUpload jobs are waiting in the queue")
            }
            else -> {
                updateSyncView(false)
            }
        }
    }

    private fun prepareForUpload() {
        if(!PlatformUtils.isNetworkAvailable(requireContext())) {
            showErrorMessageDialog(NetworkErrorState.NO_NETWORK)
            return
        }

        (requireActivity() as HomeActivity).showSnackbar(requireView(), "You're expecting too much")
    }

    private fun updateSyncView(visibility: Boolean, text: String? = null, desc: String? = null, inProgress: Boolean = false) {
        if(visibility) {
            viewBinder.syncView.visibility = View.VISIBLE
            viewBinder.syncText.text = text
            viewBinder.syncSubText.text = desc
            if(inProgress){
                viewBinder.uploadJobsButton.visibility = View.INVISIBLE
                viewBinder.syncProgressView.visibility = View.VISIBLE
            } else {
                viewBinder.syncProgressView.visibility = View.GONE
                viewBinder.uploadJobsButton.visibility = View.VISIBLE
            }
        } else {
            viewBinder.syncView.visibility = View.GONE
        }
    }

    private fun setSwipeRefreshStatus(isRefreshing: Boolean) {
        viewBinder.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun showHide404View(visibility: Boolean) {
        if (visibility) {
            viewBinder.notFoundView.root.visibility = View.VISIBLE
        } else {
            viewBinder.notFoundView.root.visibility = View.GONE
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as Workflow
        LogHelper.debug(TAG, "onItemClick()-> " +
                "workflowName: ${model.workflowName}, " +
                "workflowID: ${model.workflowID}")
        // Pass it to Home-Activity for launch
        itemClickListener.onItemClick(model.workflowID)
    }

    private fun showErrorMessageDialog(state: NetworkErrorState) {
        when(state){
            NetworkErrorState.NO_NETWORK -> {
                errorBottomSheetDialog = PlatformUtils.buildErrorBottomSheetDialog(
                    requireActivity(),
                    resources.getString(R.string.no_internet_text),
                    resources.getString(R.string.check_connection_text),
                    retryButtonClickListener
                )
            }
            NetworkErrorState.UNAUTHORIZED -> {
                errorBottomSheetDialog = PlatformUtils.buildErrorBottomSheetDialog(
                    requireActivity(),
                    resources.getString(R.string.unauthorized_text),
                    resources.getString(R.string.unauthorized_desc),
                    retryButtonClickListener,
                    resources.getString(R.string.re_login_text)
                )
            }
        }
        errorBottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        workflowViewModel.cancelAllRequests()
        _viewBinder = null
    }

}
