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

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.navArgs
import com.arm.peliondevicemanagement.databinding.FragmentWebViewBinding
import com.arm.peliondevicemanagement.helpers.LogHelper

class WebViewFragment : Fragment() {

    companion object {
        private val TAG: String = WebViewFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentWebViewBinding? = null
    private val viewBinder get() = _viewBinder!!

    private val args: WebViewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentWebViewBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        val webURL = args.webURL
        LogHelper.debug(TAG, "requestedWebURL: $webURL")

        viewBinder.inAppWebView.webViewClient = getWebViewClient()
        viewBinder.inAppWebView.settings.javaScriptEnabled = true
        viewBinder.inAppWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        viewBinder.inAppWebView.loadUrl(webURL)
    }

    private fun setSwipeRefreshViewStatus(isRefreshing: Boolean){
        viewBinder.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun getWebViewClient(): WebViewClient {

        return object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                setSwipeRefreshViewStatus(true)
                super.onPageStarted(view, url, favicon)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                setSwipeRefreshViewStatus(false)
                super.onPageFinished(view, url)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinder.inAppWebView.webViewClient = null
        _viewBinder = null
    }

}
