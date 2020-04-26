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
import android.os.Handler
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.user.Account
import com.arm.peliondevicemanagement.components.models.user.AccountProfileModel
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.components.viewmodels.LoginViewModel
import com.arm.peliondevicemanagement.constants.state.LoginState
import com.arm.peliondevicemanagement.databinding.FragmentLoginBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.AuthActivity
import com.arm.peliondevicemanagement.utils.PlatformUtils.buildErrorBottomSheetDialog
import com.arm.peliondevicemanagement.utils.PlatformUtils.isNetworkAvailable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.android.synthetic.main.layout_version.*

class LoginFragment : Fragment() {

    companion object {
        private val TAG: String = LoginFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentLoginBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var loginViewModel: LoginViewModel

    private lateinit var userEmail: String
    private lateinit var userPassword: String

    private var activeLoginActionState: LoginState = LoginState.ACTION_LOGIN

    private lateinit var errorBottomSheetDialog: BottomSheetDialog
    private lateinit var retryButtonClickListener: View.OnClickListener

    private val onBackPressedCallback = object: OnBackPressedCallback(true){
        override fun handleOnBackPressed() {
            (activity as AuthActivity).callCloseApp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentLoginBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initLogoPosition()
        setVersionName()
        runSplash()

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        setupListeners()
        setupProgressView()
    }

    private fun setupProgressView(){
        viewBinder.progressLayout.progressBar
            .indeterminateDrawable.setColorFilter(
            resources.getColor(android.R.color.white),
            android.graphics.PorterDuff.Mode.MULTIPLY)

        viewBinder.progressLayout.progressBarText.setTextColor(
            resources.getColor(android.R.color.white))
    }

    private fun setupListeners() {
        viewBinder.loginBtn.setOnClickListener {
            performLogin()
        }

        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog.dismiss()
            performLogin()
        }

        loginViewModel.getLoginActionLiveData().observe(viewLifecycleOwner, Observer { response ->
            if(!response.accounts.isNullOrEmpty()){
                processMultiAccountData(response.accounts)
                navigateToAccountsFragment()
            } else {
                processSingleAccountData(response.accessToken)
            }
        })

        loginViewModel.getUserProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            processUserProfileData(response)
        })

        loginViewModel.getAccountProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            processUserAccountProfileData(response)
            navigateToDashboardFragment()
        })

        loginViewModel.getErrorResponseLiveData().observe(viewLifecycleOwner, Observer {
            //LogHelper.debug(TAG, "error: $error")
            when(activeLoginActionState){
                LoginState.ACTION_LOGIN -> {
                    processLoginError()
                }
                LoginState.ACTION_USER_PROFILE -> {
                    processUserProfileORAccountProfileError()
                }
                LoginState.ACTION_USER_ACCOUNT_PROFILE -> {
                    processUserProfileORAccountProfileError()
                }
            }
        })
    }

    private fun runSplash(){
        Handler().postDelayed({
            changeLogoPosition()
        }, 500)
    }

    private fun initLogoPosition(){
        viewBinder.loginView.visibility = View.GONE
        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

        viewBinder.llLogo.apply {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    translationY = (displayMetrics.heightPixels / 2).toFloat() - y - height / 2
                }
            })
        }
    }

    private fun changeLogoPosition(){
        viewBinder.llLogo.animate().translationY(0f)
        Handler().postDelayed({
            doReAuthIfPossible()
        }, 500)
    }

    private fun setVersionName(){
        tvVersion.setTextColor(resources.getColor(android.R.color.white))
        tvVersion.text = (activity as AuthActivity).getAppVersion()
    }

    private fun clearPasswordTextBox() {
        viewBinder.passwordInputTxt.text.clear()
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

    private fun showHideLoginView(visibility: Boolean) = if(visibility) {
        viewBinder.loginView.visibility = View.VISIBLE
    } else {
        viewBinder.loginView.visibility = View.GONE
    }

    private fun isValidEmail(email: String): Boolean? =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean? =
        password.length > 6

    private fun validateForm(): Boolean {
        var valid = true

        viewBinder.emailInputTxt.error = when {
            viewBinder.emailInputTxt.text.isBlank() -> {
                valid = false
                "Required"
            }

            !isValidEmail(viewBinder.emailInputTxt.text.toString().trim())!! ->
                "Invalid Email"

            else -> null
        }

        viewBinder.passwordInputTxt.error = when {
            viewBinder.passwordInputTxt.text.isBlank() -> {
                valid = false
                "Required"
            }

            !isValidPassword(viewBinder.passwordInputTxt.text.toString().trim())!! ->
                "Invalid Password"

            else -> null
        }

        return valid
    }

    private fun doReAuthIfPossible() {
        // In-case of single account, re-auth will not work for now
        if(SharedPrefHelper.getSelectedAccountID().isNotEmpty()){
            showHideLoginView(false)
            // Enable feature-flag if debug-build
            if(BuildConfig.DEBUG){
                if(!SharedPrefHelper.getDeveloperOptions().isReAuthDisabled()){
                    doReAuth()
                } else {
                    LogHelper.debug(TAG, "DeveloperOptions() ReAuth->Disabled")
                    navigateToDashboardFragment()
                }
            } else {
                doReAuth()
            }
        } else {
            showHideLoginView(true)
        }
    }

    private fun doReAuth() {
        viewBinder.emailInputTxt.setText(SharedPrefHelper.getUserName())

        if(!isNetworkAvailable(requireContext())){
            LogHelper.debug(TAG, "Network not-available, go-offline")
            navigateToDashboardFragment()
            return
        }

        showHideProgressbar(true, "Re-Authenticating")

        setLoginActionState(LoginState.ACTION_LOGIN)
        LogHelper.debug(TAG, "onUserLoggedIn()->doReAuth()")
        loginViewModel.doImpersonate(SharedPrefHelper.getSelectedAccountID())
    }

    private fun performLogin() {
        if (!validateForm())
            return

        if(!isNetworkAvailable(requireContext())){
            showNoInternetDialog()
            return
        }

        showHideLoginView(false)
        showHideProgressbar(true, "Authenticating")

        userEmail = viewBinder.emailInputTxt.text.toString()
        userPassword = viewBinder.passwordInputTxt.text.toString()

        SharedPrefHelper.removeCredentials(true)
        setLoginActionState(LoginState.ACTION_LOGIN)
        SharedPrefHelper.storeSelectedUserName(userEmail)
        loginViewModel.doLogin(userEmail, userPassword)
    }

    private fun showNoInternetDialog() {
        errorBottomSheetDialog = buildErrorBottomSheetDialog(
            requireActivity(),
            resources.getString(R.string.no_internet_text),
            resources.getString(R.string.check_connection_text),
            retryButtonClickListener)
        errorBottomSheetDialog.show()
    }

    private fun processMultiAccountData(accounts: List<Account>) {
        // Store listOfAccounts as JSON in SharedPrefs
        val accountsJSON = Gson().toJson(accounts)
        //LogHelper.debug(TAG, "onUserAccounts()-> $accountsJSON")

        //Store data
        SharedPrefHelper.storeUserCredentials(userEmail, userPassword)
        SharedPrefHelper.storeMultiAccountStatus(true)
        SharedPrefHelper.storeUserAccounts(accountsJSON)
    }

    private fun processSingleAccountData(accessToken: String) {
        // Save access-token
        //LogHelper.debug(TAG, "onUserAccessToken()-> $accessToken")
        SharedPrefHelper.storeUserAccessToken(accessToken)
        // Store account-type status
        if(!SharedPrefHelper.getStoredAccounts().isNullOrBlank()){
            SharedPrefHelper.storeMultiAccountStatus(true)
        } else {
            SharedPrefHelper.storeMultiAccountStatus(false)
        }
        // Fetch user-profile
        setLoginActionState(LoginState.ACTION_USER_PROFILE)
        loginViewModel.fetchUserProfile()
    }

    private fun processUserProfileData(userProfile: UserProfile) {
        // Store user-profile data as JSON in SharedPrefs
        val profileJSON = Gson().toJson(userProfile)
        //LogHelper.debug(TAG, "onUserProfile()-> $profileJSON")
        SharedPrefHelper.storeUserProfile(profileJSON)
        SharedPrefHelper.storeSelectedUserID(userProfile.userID)
        // Now fetch selected account's profile
        setLoginActionState(LoginState.ACTION_USER_ACCOUNT_PROFILE)
        loginViewModel.fetchAccountProfile()
    }

    private fun processUserAccountProfileData(accountProfile: AccountProfileModel) {
        // Store user account-profile data as JSON in SharedPrefs
        val accountProfileJSON = Gson().toJson(accountProfile)
        //LogHelper.debug(TAG, "onAccountProfile()-> $accountProfileJSON")
        SharedPrefHelper.storeUserAccountProfile(accountProfileJSON)
    }

    private fun navigateToAccountsFragment() {
        Navigation.findNavController(viewBinder.root)
            .navigate(LoginFragmentDirections.actionLoginFragmentToAccountsFragment())
    }

    private fun navigateToDashboardFragment() {
        (requireActivity() as AuthActivity).launchHomeActivity()
    }

    private fun processLoginError() {
        (activity as AuthActivity).showSnackbar(viewBinder.root, "Failed to authenticate")
        clearPasswordTextBox()
        showHideProgressbar(false)
        showHideLoginView(true)
    }

    private fun processUserProfileORAccountProfileError() {
        SharedPrefHelper.removePassword()
        SharedPrefHelper.removeAccessToken()
        (activity as AuthActivity).showSnackbar(viewBinder.root, "Failed to authenticate")
        clearPasswordTextBox()
        showHideProgressbar(false)
        showHideLoginView(true)
    }

    private fun setLoginActionState(state: LoginState){
        activeLoginActionState = state
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        loginViewModel.cancelAllRequests()
    }

}
