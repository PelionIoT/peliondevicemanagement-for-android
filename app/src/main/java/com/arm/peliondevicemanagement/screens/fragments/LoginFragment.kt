package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.viewmodels.LoginViewModel
import com.arm.peliondevicemanagement.databinding.FragmentLoginBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.screens.activities.HostActivity
import kotlinx.android.synthetic.main.layout_version.*

class LoginFragment : Fragment() {

    private var _viewBinder: FragmentLoginBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var loginViewModel: LoginViewModel

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

        (activity as HostActivity).showHideToolbar(false)

        initLogoPosition()
        setVersionName()
        runSplash()

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        viewBinder.loginBtn.setOnClickListener {
            //performLogin()
            Navigation.findNavController(viewBinder.root).navigate(LoginFragmentDirections.actionLoginFragmentToAccountsFragment())
        }

        loginViewModel.userAccountLiveData.observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                LogHelper.debug("performLogin()", response.toString())
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
            viewBinder.loginView.visibility = View.VISIBLE
        }, 500)
    }

    private fun setVersionName(){
        tvVersion.setTextColor(resources.getColor(android.R.color.white))
        tvVersion.text = (activity as HostActivity).getAppVersion()
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

    private fun performLogin() {
        if (!validateForm())
            return

        loginViewModel.doLogin(viewBinder.emailInputTxt.text.toString(),
            viewBinder.passwordInputTxt.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
        loginViewModel.cancelAllRequests()
    }

}
