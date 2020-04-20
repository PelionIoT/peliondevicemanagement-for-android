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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.arm.peliondevicemanagement.components.adapters.AccountAdapter
import com.arm.peliondevicemanagement.databinding.FragmentAccountsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity
import com.arm.peliondevicemanagement.components.models.user.Account
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.components.viewmodels.LoginViewModel
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.user.AccountProfileModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AccountsFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = AccountsFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentAccountsBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var loginViewModel: LoginViewModel

    private var accountAdapter: AccountAdapter? = null
    private var accountModelsList = arrayListOf<Account>()

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            (activity as HostActivity).callCloseApp()
        }
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false

        override fun onQueryTextChange(newText: String?): Boolean {
            accountAdapter!!.filter.filter(newText)
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentAccountsBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupData()
        init()
        setupListeners()
    }

    private fun init() {
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        accountAdapter = AccountAdapter(accountModelsList, this)
        viewBinder.rvAccounts.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = accountAdapter
        }
        resetSearchText()
    }

    private fun setupData() {
        if(!SharedPrefHelper.getStoredAccounts().isNullOrBlank()) {
            val accountsJson = SharedPrefHelper.getStoredAccounts()
            val type = object: TypeToken<List<Account>>() {}.type
            val accounts = Gson().fromJson<ArrayList<Account>>(accountsJson, type)
            LogHelper.debug(TAG, "onStoredAccounts(): $accounts")
            accountModelsList = accounts
        }
    }

    private fun setupListeners() {
        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)

        loginViewModel.getLoginActionLiveData().observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                LogHelper.debug(TAG, "onLoginSuccess(): $response")
                processSingleAccountData(response.accessToken)
            } else {
                showHideProgressbar(false)
                showHideAccountList(true)
                (activity as HostActivity).showSnackbar(viewBinder.root ,"Failed to authenticate")
            }
        })

        loginViewModel.getUserProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                processUserProfileData(response)
            } else {
                showHideProgressbar(false)
                showHideAccountList(true)
                (activity as HostActivity).showSnackbar(viewBinder.root, "Failed to authenticate")
            }
        })

        loginViewModel.getAccountProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                processUserAccountProfileData(response)
                navigateToDashboardFragment()
            } else {
                showHideProgressbar(false)
                showHideAccountList(true)
                (activity as HostActivity).showSnackbar(viewBinder.root, "Failed to authenticate")
            }
        })
    }

    private fun showHideProgressbar(visibility: Boolean, text: String = ""){
        if(visibility) {
            viewBinder.progressLayout.root.visibility = View.VISIBLE
            if(text.isNotEmpty())
                viewBinder.progressLayout.progressBarText.text = text
        } else {
            viewBinder.progressLayout.root.visibility = View.GONE
        }
    }

    private fun showHideAccountList(visibility: Boolean) = if(visibility){
        viewBinder.searchBar.root.visibility = View.VISIBLE
        viewBinder.rvAccounts.visibility = View.VISIBLE
    } else {
        viewBinder.searchBar.root.visibility = View.GONE
        viewBinder.rvAccounts.visibility = View.GONE
    }

    override fun onItemClick(data: Any) {
        val model = data as Account
        LogHelper.debug(TAG, "onItemClick()-> " +
                "accountName: ${model.accountName}, " +
                "accountID: ${model.accountID}")

        showHideAccountList(false)
        showHideProgressbar(true)

        SharedPrefHelper.storeSelectedAccountID(model.accountID)
        SharedPrefHelper.storeSelectedAccountName(model.accountName)
        handleLoginORGoImpersonate(model.accountID)
    }

    private fun handleLoginORGoImpersonate(accountID: String) {
        if(!SharedPrefHelper.getUserPassword().isNullOrBlank()){
            val username = SharedPrefHelper.getUserName()
            val password = SharedPrefHelper.getUserPassword()

            /*LogHelper.debug(TAG, "onHandleLogin(): " +
                    "email: $username, " +
                    "password: $password, " +
                    "accountID: $accountID")*/

            loginViewModel.doLogin(username, password!!, accountID)
        } else {
            LogHelper.debug(TAG, "onGoImpersonate(): accountID: $accountID")
            loginViewModel.doImpersonate(accountID)
        }
    }

    private fun processSingleAccountData(accessToken: String) {
        // Save access-token
        LogHelper.debug(TAG, "onUserAccessToken()-> $accessToken")
        SharedPrefHelper.storeMultiAccountStatus(true)
        SharedPrefHelper.storeUserAccessToken(accessToken)
        // Now fetch user-profile
        loginViewModel.fetchUserProfile()
    }

    private fun processUserProfileData(userProfile: UserProfile) {
        // Store user-profile data as JSON in SharedPrefs
        val profileJSON = Gson().toJson(userProfile)
        LogHelper.debug(TAG, "onUserProfile()-> $profileJSON")
        SharedPrefHelper.storeUserProfile(profileJSON)
        SharedPrefHelper.storeSelectedUserID(userProfile.userID)
        // Now fetch selected account's profile
        loginViewModel.fetchAccountProfile()
    }

    private fun processUserAccountProfileData(accountProfile: AccountProfileModel) {
        // Store user account-profile data as JSON in SharedPrefs
        val accountProfileJSON = Gson().toJson(accountProfile)
        LogHelper.debug(TAG, "onAccountProfile()-> $accountProfileJSON")
        SharedPrefHelper.storeUserAccountProfile(accountProfileJSON)
        if(!SharedPrefHelper.getUserPassword().isNullOrBlank()) {
            SharedPrefHelper.removePassword()
        }
    }

    private fun navigateToDashboardFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(AccountsFragmentDirections.actionAccountsFragmentToDashboardFragment())
    }

    private fun resetSearchText() =
        viewBinder.searchBar.searchTextBox.setQuery("", false)

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        loginViewModel.cancelAllRequests()
    }

}
