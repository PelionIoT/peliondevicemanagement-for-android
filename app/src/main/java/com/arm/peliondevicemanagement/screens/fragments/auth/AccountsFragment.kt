/*
 * Copyright 2020 ARM Ltd.
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

package com.arm.peliondevicemanagement.screens.fragments.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.adapters.AccountAdapter
import com.arm.peliondevicemanagement.databinding.FragmentAccountsBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.components.models.user.Account
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.components.viewmodels.LoginViewModel
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.branding.BrandingImage
import com.arm.peliondevicemanagement.components.models.user.AccountProfile
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_BRAND_LOGO
import com.arm.peliondevicemanagement.constants.BrandingTheme
import com.arm.peliondevicemanagement.constants.state.NetworkErrorState
import com.arm.peliondevicemanagement.screens.activities.AuthActivity
import com.arm.peliondevicemanagement.services.data.BrandingImageResponse
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    private lateinit var activeAccountModel: Account
    private var errorBottomSheetDialog: BottomSheetDialog? = null
    private lateinit var retryButtonClickListener: View.OnClickListener

    private lateinit var networkRequestState: NetworkErrorState

    private val authArgsBundle: AccountsFragmentArgs by navArgs()

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            (activity as AuthActivity).callCloseApp()
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
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
        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
            when(networkRequestState){
                NetworkErrorState.NO_NETWORK -> {
                    processSelectedAccount(activeAccountModel)
                }
                NetworkErrorState.UNAUTHORIZED -> {
                    navigateToLogin()
                }
            }
        }

        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)

        loginViewModel.getLoginActionLiveData().observe(viewLifecycleOwner, Observer { response ->
            LogHelper.debug(TAG, "onLoginSuccess(): $response")
            processSingleAccountData(response.accessToken)
        })

        loginViewModel.getUserProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            processUserProfileData(response)
        })

        loginViewModel.getAccountProfileLiveData().observe(viewLifecycleOwner, Observer { response ->
            processUserAccountProfileData(response)
        })

        loginViewModel.getAccountBrandingImagesLiveData().observe(viewLifecycleOwner, Observer { response ->
            processAccountBrandingImagesData(response)
            navigateToDashboardFragment()
        })

        loginViewModel.getErrorResponseLiveData().observe(viewLifecycleOwner, Observer { error ->
            if(error != null){
                if(error.errorCode == 403){
                    // In-case of error, skip branding and proceed
                    // {"code":403,"message":"Actor must be an administrator of the account.","type":"access_denied"}
                    LogHelper.debug(TAG, "AccountBrandingLogo: N/A, Actor must be an administrator of the account")
                    SharedPrefHelper.storeSelectedAccountBrandingLogoURL("")
                    navigateToDashboardFragment()
                } else {
                    processErrorResponse()
                }
            } else {
                processTimeout()
            }
        })
    }

    private fun processTimeout() {
        SharedPrefHelper.storeSelectedAccountID(null)
        showHideProgressbar(false)
        showHideAccountList(true)
        (requireActivity() as AuthActivity).showSnackbar(requireView(), "Time-out, try again.")
    }

    private fun processErrorResponse() {
        showHideProgressbar(false)
        showHideAccountList(true)
        networkRequestState = NetworkErrorState.UNAUTHORIZED
        showErrorMessageDialog(NetworkErrorState.UNAUTHORIZED)
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

    private fun navigateToLogin() {
        (requireActivity() as AuthActivity).navigateToLogin()
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
        LogHelper.debug(
            TAG, "onItemClick()-> " +
                "accountName: ${model.accountName}, " +
                "accountID: ${model.accountID}")

        activeAccountModel = model
        processSelectedAccount(model)
    }

    private fun processSelectedAccount(account: Account) {
        if(SharedPrefHelper.getSelectedAccountID().isNotEmpty()
            && account.accountID == SharedPrefHelper.getSelectedAccountID()){
            navigateToDashboardFragment()
        } else {
            if(!PlatformUtils.isNetworkAvailable(requireContext())){
                networkRequestState = NetworkErrorState.NO_NETWORK
                showErrorMessageDialog(NetworkErrorState.NO_NETWORK)
                return
            }

            showHideAccountList(false)
            showHideProgressbar(true)

            SharedPrefHelper.storeSelectedAccountID(account.accountID)
            SharedPrefHelper.storeSelectedAccountName(account.accountName)
            handleLoginORGoImpersonate(account.accountID)
        }
    }

    private fun handleLoginORGoImpersonate(accountID: String) {
        if(authArgsBundle.authArgs != null) {
            val username = authArgsBundle.authArgs!!.email
            val password = authArgsBundle.authArgs!!.password

            // If twoFA enabled then use this one, else standard login
            if(authArgsBundle.authArgs!!.isOTPRequired){
                val otp = authArgsBundle.authArgs!!.otp

                /*LogHelper.debug(TAG, "onHandleLogin(): " +
                        "email: $username, " +
                        "password: $password, " +
                        "accountID: $accountID, " +
                        "otpCode: $otp")*/

                LogHelper.debug(TAG, "Attempting for login-window with 2Auth-OTPCode: $otp")
                loginViewModel.do2AuthLogin(username, password, otpCode = otp,
                    captchaID = null, captchaCode = null, accountID = accountID)

            } else {
                /*LogHelper.debug(TAG, "onHandleLogin(): " +
                    "email: $username, " +
                    "password: $password, " +
                    "accountID: $accountID")*/

                loginViewModel.doLogin(username, password, accountID)
            }

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

    private fun processUserAccountProfileData(accountProfile: AccountProfile) {
        // Store user account-profile data as JSON in SharedPrefs
        val accountProfileJSON = Gson().toJson(accountProfile)
        LogHelper.debug(TAG, "onAccountProfile()-> $accountProfileJSON")
        SharedPrefHelper.storeUserAccountProfile(accountProfileJSON)

        // Now fetch account-branding-images
        val accountID = SharedPrefHelper.getSelectedAccountID()
        val activeTheme = if(SharedPrefHelper.isDarkThemeEnabled()){
            BrandingTheme.DARK
        } else {
            BrandingTheme.LIGHT
        }
        loginViewModel.fetchAccountBrandingImages(accountID, activeTheme)
    }

    private fun processAccountBrandingImagesData(brandingResponse: BrandingImageResponse) {
        //LogHelper.debug(TAG, "Branding-Images-> ${brandingResponse.brandingImages}")

        // Process brand-logo for now
        val brandLogoIndex = brandingResponse.brandingImages
            .indexOf(BrandingImage("", KEY_BRAND_LOGO))
        val brandLogoURL = brandingResponse.brandingImages[brandLogoIndex].imageURL
        if(brandLogoURL != null){
            //LogHelper.debug(TAG, "AccountBrandingLogo: Available, url: $brandLogoURL")
            SharedPrefHelper.storeSelectedAccountBrandingLogoURL(brandLogoURL)
            LogHelper.debug(TAG, "AccountBrandingLogo: Available, saved")
        } else {
            LogHelper.debug(TAG, "AccountBrandingLogo: Not available")
            SharedPrefHelper.storeSelectedAccountBrandingLogoURL("")
        }
    }

    private fun navigateToDashboardFragment() {
        (requireActivity() as AuthActivity).launchChooseFeatureActivity()
    }

    private fun showErrorMessageDialog(state: NetworkErrorState) {
        if(errorBottomSheetDialog != null) {
            // If previous dialog is already visible
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
        }

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
        errorBottomSheetDialog!!.show()
    }

    private fun resetSearchText() =
        viewBinder.searchBar.searchTextBox.setQuery("", false)

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        loginViewModel.cancelAllRequests()
    }

}
