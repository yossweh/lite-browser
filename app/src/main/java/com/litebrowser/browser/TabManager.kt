package com.litebrowser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.litebrowser.adblock.AdBlocker

data class Tab(
    val id: String,
    val webView: WebView,
    var title: String = "New Tab",
    var url: String = ""
)

class TabManager(
    private val context: Context,
    private val container: FrameLayout,
    private val progressBar: ProgressBar,
    private val adBlocker: AdBlocker?,
    private val onPageStarted: ((String) -> Unit)?,
    private val onPageFinished: ((String?) -> Unit)?,
    private val onTitleReceived: ((String?) -> Unit)?,
    private val onProgressChanged: ((Int) -> Unit)?
) {
    private val tabs = mutableListOf<Tab>()
    private var activeTab: Tab? = null
    private var activeTabIndex = -1
    private var isAdBlockEnabled = true
    private var isDesktopMode = false
    private var darkMode = "auto"

    @SuppressLint("SetJavaScriptEnabled")
    fun createTab(url: String? = null): Tab {
        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        val tabId = "tab_${System.currentTimeMillis()}"
        val tab = Tab(id = tabId, webView = webView, url = url ?: "")

        setupWebView(webView, tab)
        tabs.add(tab)
        container.addView(webView)

        return tab
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, tab: Tab) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Apply user agent
            userAgentString = if (isDesktopMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
        }

        // Apply dark mode safely
        applyDarkMode(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?, request: WebResourceRequest?
            ): WebResourceResponse? {
                if (isAdBlockEnabled && request != null && adBlocker != null) {
                    if (adBlocker.isAd(request.url.toString())) {
                        return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (isActiveTab(tab)) {
                    progressBar.visibility = View.VISIBLE
                    url?.let {
                        tab.url = it
                        onPageStarted?.invoke(it)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (isActiveTab(tab)) {
                    progressBar.visibility = View.GONE
                    url?.let { tab.url = it }
                    onPageFinished?.invoke(url)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("intent:")) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (isActiveTab(tab)) {
                    progressBar.progress = newProgress
                    onProgressChanged?.invoke(newProgress)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                tab.title = title ?: "New Tab"
                if (isActiveTab(tab)) {
                    onTitleReceived?.invoke(title)
                }
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isActiveTab(tab: Tab): Boolean = activeTab?.id == tab.id

    fun switchToTab(index: Int) {
        if (index < 0 || index >= tabs.size) return

        // Hide current tab
        activeTab?.webView?.visibility = View.GONE

        // Show new tab
        activeTabIndex = index
        activeTab = tabs[index]
        activeTab?.webView?.visibility = View.VISIBLE

        // Update URL bar
        activeTab?.url?.let { onPageStarted?.invoke(it) }
        activeTab?.title?.let { onTitleReceived?.invoke(it) }
    }

    fun switchToTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index >= 0) switchToTab(index)
    }

    fun closeTab(index: Int) {
        if (index < 0 || index >= tabs.size) return

        val tab = tabs[index]
        tab.webView.stopLoading()
        tab.webView.destroy()
        container.removeView(tab.webView)
        tabs.removeAt(index)

        // Switch to another tab if we closed the active one
        if (activeTabIndex == index) {
            if (tabs.isEmpty()) {
                // Create a new empty tab
                val newTab = createTab()
                switchToTab(0)
                newTab.webView.loadUrl("https://www.google.com")
            } else {
                val newIndex = if (index >= tabs.size) tabs.size - 1 else index
                switchToTab(newIndex)
            }
        } else if (activeTabIndex > index) {
            activeTabIndex--
        }
    }

    fun closeTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index >= 0) closeTab(index)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun loadUrl(url: String) {
        val tab = activeTab ?: return
        tab.webView.loadUrl(url)
        tab.url = url
    }

    fun getCurrentUrl(): String? = activeTab?.url

    fun getCurrentWebView(): WebView? = activeTab?.webView

    fun getTabs(): List<Tab> = tabs.toList()

    fun getTabCount(): Int = tabs.size

    fun getActiveTabIndex(): Int = activeTabIndex

    fun canGoBack(): Boolean = activeTab?.webView?.canGoBack() == true

    fun goBack() { activeTab?.webView?.goBack() }

    fun canGoForward(): Boolean = activeTab?.webView?.canGoForward() == true

    fun goForward() { activeTab?.webView?.goForward() }

    fun reload() { activeTab?.webView?.reload() }

    fun onResume() { activeTab?.webView?.onResume() }

    fun onPause() { activeTab?.webView?.onPause() }

    @SuppressLint("SetJavaScriptEnabled")
    fun setAdBlockEnabled(enabled: Boolean) {
        isAdBlockEnabled = enabled
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setDesktopMode(enabled: Boolean) {
        isDesktopMode = enabled
        tabs.forEach { tab ->
            tab.webView.settings.userAgentString = if (enabled) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setDarkMode(mode: String) {
        darkMode = mode
        tabs.forEach { applyDarkMode(it.webView) }
    }

    private fun applyDarkMode(webView: WebView) {
        try {
            when (darkMode) {
                "on" -> {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        webView.settings.isAlgorithmicDarkeningAllowed = true
                    } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                        }
                    }
                }
                "off" -> {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        webView.settings.isAlgorithmicDarkeningAllowed = false
                    } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                        }
                    }
                }
                "auto" -> {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        webView.settings.isAlgorithmicDarkeningAllowed = true
                    } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_AUTO)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        tabs.forEach { tab ->
            tab.webView.stopLoading()
            tab.webView.destroy()
        }
        tabs.clear()
        activeTab = null
        activeTabIndex = -1
    }
}
