package com.arm.peliondevicemanagement.screens.activities

import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.ViewTreeObserver
import com.arm.peliondevicemanagement.databinding.ActivityLaunchBinding
import kotlinx.android.synthetic.main.layout_version.*

class LaunchActivity : BaseActivity() {

    private lateinit var viewBinder: ActivityLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initLogoPosition()
        setVersionName()
        runSplash()
    }

    private fun runSplash(){
        Handler().postDelayed(Runnable {
            changeLogoPosition()
        }, 500)
    }

    private fun initLogoPosition(){
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

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
    }

    private fun setVersionName(){
        tvVersion.text = getAppVersion()
    }


}
