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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.models.branding.BrandingImage
import com.arm.peliondevicemanagement.components.models.user.Account
import com.arm.peliondevicemanagement.components.models.user.AccountProfile
import com.arm.peliondevicemanagement.components.models.user.AuthModel
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.components.viewmodels.LoginViewModel
import com.arm.peliondevicemanagement.constants.APIConstants
import com.arm.peliondevicemanagement.constants.BrandingTheme
import com.arm.peliondevicemanagement.constants.state.LoginState
import com.arm.peliondevicemanagement.databinding.FragmentTwoFactorAuthBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.screens.activities.AuthActivity
import com.arm.peliondevicemanagement.services.data.BrandingImageResponse
import com.arm.peliondevicemanagement.services.data.CaptchaResponse
import com.arm.peliondevicemanagement.services.data.ErrorFields
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.peliondevicemanagement.utils.PlatformUtils
import com.arm.peliondevicemanagement.utils.PlatformUtils.getBitMapFromString
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson

class TwoFactorAuthFragment : Fragment() {

    companion object {
        private val TAG: String = TwoFactorAuthFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentTwoFactorAuthBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val onBackPressedCallback = object: OnBackPressedCallback(true){
        override fun handleOnBackPressed() {
            navigateBackToLoginFragment()
        }
    }

    private var activeLoginActionState: LoginState = LoginState.ACTION_LOGIN
    private var errorBottomSheetDialog: BottomSheetDialog? = null
    private lateinit var retryButtonClickListener: View.OnClickListener

    private lateinit var loginViewModel: LoginViewModel

    private var activeCaptchaID = ""
    private lateinit var otpCode: String
    private lateinit var userPassword: String

    private val args: TwoFactorAuthFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentTwoFactorAuthBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Add onBackPressed-callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        //LogHelper.debug(TAG, "Received bundle of ${args.authArgs}")

        if(args.authArgs.isCaptchaRequired){
            loginViewModel.fetchCaptcha()
            showHideCaptchaView(true)
            showHidePasswordView(true)
        }

        if(!args.authArgs.isOTPRequired){
            showHideOTPView(false)
        }

        setupListeners()
        setupProgressView()
    }

    private fun setupListeners() {
        viewBinder.verifyBtn.setOnClickListener {
            performLogin()
        }

        viewBinder.refreshCaptchaButton.setOnClickListener {
            loginViewModel.fetchCaptcha()
        }

        retryButtonClickListener = View.OnClickListener {
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
            performLogin()
        }

        loginViewModel.getLoginActionLiveData().observe(viewLifecycleOwner, Observer { response ->
            // Now process the login response
            if(!response.accounts.isNullOrEmpty()){
                processMultiAccountData(response.accounts)

                val authArgs: AuthModel
                if(args.authArgs.isCaptchaRequired && args.authArgs.isOTPRequired) {
                    authArgs = AuthModel(args.authArgs.email,
                        userPassword,
                        args.authArgs.accountID,
                        isOTPRequired = true,
                        isCaptchaRequired = false,
                        otp = otpCode,
                        captcha = null
                    )
                    navigateToAccountsFragment(authArgs)
                } else if(args.authArgs.isOTPRequired) {
                    authArgs = AuthModel(args.authArgs.email,
                        args.authArgs.password,
                        args.authArgs.accountID,
                        isOTPRequired = true,
                        isCaptchaRequired = false,
                        otp = otpCode,
                        captcha = null
                    )
                    navigateToAccountsFragment(authArgs)
                } else if(args.authArgs.isCaptchaRequired) {
                    authArgs = AuthModel(args.authArgs.email,
                        userPassword,
                        args.authArgs.accountID,
                        isOTPRequired = false,
                        isCaptchaRequired = false,
                        otp = null,
                        captcha = null
                    )
                    navigateToAccountsFragment(authArgs)
                }
            } else {
                processSingleAccountData(response.accessToken)
            }
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

        loginViewModel.getCaptchaLiveData().observe(viewLifecycleOwner, Observer { captchaResponse ->
            if(captchaResponse != null){
                processAndSetCaptchaImage(captchaResponse)
            }
        })

        loginViewModel.getErrorResponseLiveData().observe(viewLifecycleOwner, Observer { error ->
            when(activeLoginActionState){
                LoginState.ACTION_LOGIN -> {
                    processLoginError(error)
                }
                LoginState.ACTION_USER_PROFILE -> {
                    processUserProfileORAccountProfileError()
                }
                LoginState.ACTION_USER_ACCOUNT_PROFILE -> {
                    processUserProfileORAccountProfileError()
                }
                LoginState.ACTION_ACCOUNT_BRANDING -> {
                    // In-case of error, skip branding and proceed
                    // {"code":403,"message":"Actor must be an administrator of the account.","type":"access_denied"}
                    LogHelper.debug(TAG, "AccountBrandingLogo: N/A, Actor must be an administrator of the account")
                    SharedPrefHelper.storeSelectedAccountBrandingLogoURL("")
                    navigateToDashboardFragment()
                }
            }
        })
    }

    private fun isValidPassword(password: String): Boolean? =
        password.length > 6

    private fun isValidOTP(code: String): Boolean? =
        code.length >= 6

    private fun validateForm(): Boolean {
        var valid = true

        if(args.authArgs.isCaptchaRequired){
            viewBinder.captchaInputTxt.error = when {
                viewBinder.captchaInputTxt.text.isBlank() -> {
                    valid = false
                    "Required"
                }

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
        }

        if(args.authArgs.isOTPRequired){
            viewBinder.otpInputTxt.error = when {
                viewBinder.otpInputTxt.text.isBlank() -> {
                    valid = false
                    "Required"
                }

                !isValidOTP(viewBinder.otpInputTxt.text.toString().trim())!! -> {
                    valid = false
                    "Invalid Code"
                }

                else -> null
            }
        }

        return valid
    }

    private fun performLogin() {
        if (!validateForm())
            return

        // Hide keyboard, if not hidden
        PlatformUtils.hideKeyboard(requireActivity())

        if(!PlatformUtils.isNetworkAvailable(requireContext())){
            showNoInternetDialog()
            return
        }

        val userEmail = args.authArgs.email
        userPassword = viewBinder.passwordInputTxt.text.toString()
        val accountID = args.authArgs.accountID
        val captchaCode = viewBinder.captchaInputTxt.text.toString()
        otpCode = viewBinder.otpInputTxt.text.toString()

        // Hide
        showHideOTPView(false)
        showHidePasswordView(false)
        showHideCaptchaView(false)
        showHideVerifyButton(false)
        // Show
        showHideProgressbar(true, "Authenticating")
        // Clear
        clearCodeTextBoxes()

        if(args.authArgs.isCaptchaRequired && args.authArgs.isOTPRequired) {

            // Do both
            LogHelper.debug(TAG, "Attempting for login-window with 2Auth OTPCode: $otpCode, CaptchaCode: $captchaCode")
            loginViewModel.do2AuthLogin(userEmail, userPassword, otpCode = otpCode,
                captchaID = activeCaptchaID, captchaCode = captchaCode, accountID = accountID)

        } else if(args.authArgs.isOTPRequired) {

            // Do OTP Only
            LogHelper.debug(TAG, "Attempting for login-window with 2Auth-OTPCode: $otpCode")
            loginViewModel.do2AuthLogin(userEmail, args.authArgs.password, otpCode = otpCode,
                captchaID = null, captchaCode = null, accountID = accountID)

        } else if(args.authArgs.isCaptchaRequired) {

            // Do Captcha Only
            LogHelper.debug(TAG, "Attempting for login-window with 2Auth-CaptchaCode: $captchaCode")
            loginViewModel.do2AuthLogin(userEmail, userPassword,
                otpCode = null, captchaID = activeCaptchaID,
                captchaCode = captchaCode, accountID = accountID)

        }
    }

    private fun processMultiAccountData(accounts: List<Account>) {
        LogHelper.debug(TAG, "LoginMode: MultiAccount()")
        // Store listOfAccounts as JSON in SharedPrefs
        val accountsJSON = Gson().toJson(accounts)
        //LogHelper.debug(TAG, "onUserAccounts()-> $accountsJSON")

        // Store accounts-data
        SharedPrefHelper.storeMultiAccountStatus(true)
        SharedPrefHelper.storeUserAccounts(accountsJSON)
    }

    private fun processSingleAccountData(accessToken: String) {
        LogHelper.debug(TAG, "LoginMode: SingleAccount()")
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
        LogHelper.debug(TAG, "onUserProfile()-> $profileJSON")
        SharedPrefHelper.storeUserProfile(profileJSON)
        SharedPrefHelper.storeSelectedUserID(userProfile.userID)
        // Now fetch selected account's profile
        setLoginActionState(LoginState.ACTION_USER_ACCOUNT_PROFILE)
        loginViewModel.fetchAccountProfile()
    }

    private fun processUserAccountProfileData(accountProfile: AccountProfile) {
        // Store user account-profile data as JSON in SharedPrefs
        val accountProfileJSON = Gson().toJson(accountProfile)
        LogHelper.debug(TAG, "onAccountProfile()-> $accountProfileJSON")
        SharedPrefHelper.storeUserAccountProfile(accountProfileJSON)

        // Store account-information
        SharedPrefHelper.storeSelectedAccountID(accountProfile.accountID)
        SharedPrefHelper.storeSelectedAccountName(accountProfile.accountName)

        // Now fetch account-branding-images
        setLoginActionState(LoginState.ACTION_ACCOUNT_BRANDING)
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
            .indexOf(BrandingImage("", APIConstants.KEY_BRAND_LOGO))
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

    private fun processAndSetCaptchaImage(captchaResponse: CaptchaResponse){
        activeCaptchaID = captchaResponse.captchaID
        val captchaImage = getBitMapFromString(captchaResponse.captchaBytes)
        Glide.with(requireView())
            .load(captchaImage)
            .placeholder(R.drawable.ic_image_dark)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.ic_image_dark)
            .into(viewBinder.imgCaptchaView)
    }

    private fun navigateBackToLoginFragment() {
        LogHelper.debug(TAG, "->twoAuth() Interrupted, going back to login")
        Navigation.findNavController(viewBinder.root)
            .navigate(TwoFactorAuthFragmentDirections
                .actionTwoFactorAuthFragmentToLoginFragment())
    }

    private fun navigateToAccountsFragment(authArgs: AuthModel) {
        Navigation.findNavController(viewBinder.root)
            .navigate(TwoFactorAuthFragmentDirections
                .actionTwoFactorAuthFragmentToAccountsFragment(authArgs))
    }

    private fun navigateToDashboardFragment() {
        (requireActivity() as AuthActivity).launchChooseFeatureActivity()
    }

    private fun processLoginError(error: ErrorResponse? = null) {
        if(error != null){
            if(!error.errorFields.isNullOrEmpty()){
                processErrorFieldsAndTakeAction(error.errorFields!!)
            } else {
                if(error.errorCode == 400 && error.errorType == APIConstants.KEY_VALIDATION_ERROR){
                    showIncorrectAttemptLimitReached()
                } else {
                    showSnackbar("Failed to authenticate")
                    navigateBackToLoginFragment()
                }
            }
        } else {
            showSnackbar("Failed to authenticate")
            navigateBackToLoginFragment()
        }
    }

    private fun processErrorFieldsAndTakeAction(errorFields: List<ErrorFields>) {
        args.authArgs.isOTPRequired = false
        args.authArgs.isCaptchaRequired = false

        errorFields.forEach { errorField ->
            if(errorField.name == APIConstants.KEY_OTP_TOKEN){
                LogHelper.debug(TAG, "Invalid OTP-Code")

                viewBinder.otpInputTxt.error = "Invalid OTP-Code"
                showHideOTPView(true)
                showSnackbar("Invalid OTP-Code, try again")
                args.authArgs.isOTPRequired = true
            }

            if(errorField.name == APIConstants.KEY_CAPTCHA){
                LogHelper.debug(TAG, "Invalid Captcha")

                loginViewModel.fetchCaptcha()
                viewBinder.captchaInputTxt.error = "Invalid Captcha"
                showHideCaptchaView(true)
                showHidePasswordView(true)
                showSnackbar("Invalid Captcha, try again")
                args.authArgs.isCaptchaRequired = true
            }
        }

        showHideProgressbar(false)
        showHideVerifyButton(true)
    }

    private fun processUserProfileORAccountProfileError() {
        SharedPrefHelper.removeAccessToken()
        (activity as AuthActivity).showSnackbar(viewBinder.root, "Unknown-error, try again")
        navigateBackToLoginFragment()
    }

    private fun showIncorrectAttemptLimitReached() {
        MaterialAlertDialogBuilder(context)
            .setTitle(resources.getString(R.string.unauthorized_limit_reached_text))
            .setMessage(resources.getString(R.string.unauthorized_limit_reached_desc))
            .setPositiveButton(resources.getString(R.string.got_it_text)) { dialog, _ ->
                dialog.dismiss()
                navigateBackToLoginFragment()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun setLoginActionState(state: LoginState){
        activeLoginActionState = state
    }

    private fun showSnackbar(message: String) {
        (activity as AuthActivity)
            .showSnackbar(viewBinder.root, message)
    }

    private fun clearCodeTextBoxes() {
        viewBinder.otpInputTxt.text.clear()
        viewBinder.captchaInputTxt.text.clear()
        viewBinder.passwordInputTxt.text.clear()
    }

    private fun setupProgressView(){
        @Suppress("DEPRECATION")
        viewBinder.progressLayout.progressBar
            .indeterminateDrawable.setColorFilter(
                resources.getColor(android.R.color.white),
                android.graphics.PorterDuff.Mode.MULTIPLY)

        viewBinder.progressLayout.progressBarText.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white))
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

    private fun showHideVerifyButton(visibility: Boolean) = if(visibility){
        viewBinder.verifyBtn.visibility = View.VISIBLE
    } else {
        viewBinder.verifyBtn.visibility = View.GONE
    }

    private fun showHidePasswordView(visibility: Boolean) = if(visibility) {
        viewBinder.txtPasswordHeader.visibility = View.VISIBLE
    } else {
        viewBinder.txtPasswordHeader.visibility = View.GONE
    }

    private fun showHideOTPView(visibility: Boolean) = if(visibility) {
        viewBinder.txtOTPHeader.visibility = View.VISIBLE
    } else {
        viewBinder.txtOTPHeader.visibility = View.GONE
    }

    private fun showHideCaptchaView(visibility: Boolean) = if(visibility){
        viewBinder.imgCaptchaHeader.visibility = View.VISIBLE
        viewBinder.txtCaptchaHeader.visibility = View.VISIBLE
    } else {
        viewBinder.imgCaptchaHeader.visibility = View.GONE
        viewBinder.txtCaptchaHeader.visibility = View.GONE
    }

    private fun showNoInternetDialog() {
        if(errorBottomSheetDialog != null) {
            // If previous dialog is already visible
            errorBottomSheetDialog!!.dismiss()
            errorBottomSheetDialog = null
        }

        errorBottomSheetDialog = PlatformUtils.buildErrorBottomSheetDialog(
            requireActivity(),
            resources.getString(R.string.no_internet_text),
            resources.getString(R.string.check_connection_text),
            retryButtonClickListener
        )
        errorBottomSheetDialog!!.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginViewModel.cancelAllRequests()
        _viewBinder = null
    }

}
