package com.litebrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.AdView
import com.litebrowser.adblock.AdBlocker
import com.litebrowser.ads.AdManager
import com.litebrowser.browser.TabManager
import com.litebrowser.settings.SettingsActivity
import com.litebrowser.utils.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnTabs: ImageButton
    private lateinit var tvTabCount: TextView
    private lateinit var adContainer: FrameLayout

    // Bottom navigation
    private lateinit var btnHome: ImageButton
    private lateinit var btnBookmarks: ImageButton
    private lateinit var btnHistory: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnSettings: ImageButton

    private lateinit var tabManager: TabManager
    private lateinit var adBlocker: AdBlocker
    private lateinit var prefsManager: PrefsManager
    private lateinit var adManager: AdManager

    private var isAdBlockEnabled = true
    private var bannerAdView: AdView? = null

    companion object {
        private const val REQUEST_SETTINGS = 1001
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply dark mode before setContentView
        applyDarkMode()
        
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnTabs = findViewById(R.id.btnTabs)
        tvTabCount = findViewById(R.id.tvTabCount)
        adContainer = findViewById(R.id.adContainer)

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome)
        btnBookmarks = findViewById(R.id.btnBookmarks)
        btnHistory = findViewById(R.id.btnHistory)
        btnShare = findViewById(R.id.btnShare)
        btnSettings = findViewById(R.id.btnSettings)

        // Initialize managers
        tabManager = TabManager(this)
        adBlocker = AdBlocker(this)
        prefsManager = PrefsManager(this)
        adManager = AdManager(this)

        isAdBlockEnabled = prefsManager.isAdBlockEnabled()

        setupWebView()
        setupListeners()
        setupAds()

        // Load homepage
        val intentUrl = intent?.data?.toString()
        if (intentUrl != null) {
            loadUrl(intentUrl)
        } else {
            loadUrl(prefsManager.getHomepage())
        }
    }

    private fun applyDarkMode() {
        when (prefsManager.getDarkMode()) {
            "on" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun setupAds() {
        adManager.initialize()
        bannerAdView = adManager.createBannerAd()
        adContainer.addView(bannerAdView)
        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        bannerAdView?.loadAd(adRequest)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = prefsManager.isJavascriptEnabled()
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
            userAgentString = if (prefsManager.isDesktopMode()) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                prefsManager.getUserAgent()
            }
            
            // Apply dark mode to WebView
            applyWebViewDarkMode(this)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (isAdBlockEnabled && request != null) {
                    if (adBlocker.isAd(request.url.toString())) {
                        return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                url?.let { urlBar.setText(it) }
                updateNavigationButtons()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                updateNavigationButtons()
                adManager.onPageLoaded()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("intent:")) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                progressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                supportActionBar?.title = title ?: getString(R.string.app_name)
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyWebViewDarkMode(settings: WebSettings) {
        // Enable dark mode for WebView content
        if (prefsManager.getDarkMode() == "on") {
            // Force dark mode on website content
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                settings.isAlgorithmicDarkeningAllowed = true
            }
        } else if (prefsManager.getDarkMode() == "off") {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                settings.isAlgorithmicDarkeningAllowed = false
            }
        }
        // For "auto", let system decide
    }

    private fun setupListeners() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = urlBar.text.toString().trim()
                loadUrl(input)
                true
            } else false
        }

        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlBar.selectAll()
        }

        btnBack.setOnClickListener { webView.goBack() }
        btnForward.setOnClickListener { webView.goForward() }
        btnTabs.setOnClickListener { showTabsDialog() }
        swipeRefresh.setOnRefreshListener { webView.reload() }

        btnHome.setOnClickListener { loadUrl(prefsManager.getHomepage()) }
        btnBookmarks.setOnClickListener { showBookmarks() }
        btnHistory.setOnClickListener { showHistory() }
        btnShare.setOnClickListener { sharePage() }
        btnSettings.setOnClickListener { 
            startActivityForResult(Intent(this, SettingsActivity::class.java), REQUEST_SETTINGS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
            // Reload settings and apply changes
            isAdBlockEnabled = prefsManager.isAdBlockEnabled()
            
            // Apply dark mode changes immediately
            applyDarkMode()
            
            // Apply user agent changes
            webView.settings.userAgentString = if (prefsManager.isDesktopMode()) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                prefsManager.getUserAgent()
            }
            
            // Apply JavaScript setting
            webView.settings.javaScriptEnabled = prefsManager.isJavascriptEnabled()
            
            // Apply WebView dark mode
            applyWebViewDarkMode(webView.settings)
            
            // Reload current page to apply changes
            webView.reload()
        }
    }

    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${Uri.encode(input)}"
        }
        webView.loadUrl(url)
        urlBar.setText(url)
    }

    private fun updateNavigationButtons() {
        btnBack.alpha = if (webView.canGoBack()) 1.0f else 0.3f
        btnForward.alpha = if (webView.canGoForward()) 1.0f else 0.3f
    }

    private fun showTabsDialog() {
        val tabs = tabManager.getTabs()
        val currentUrl = webView.url ?: ""

        AlertDialog.Builder(this)
            .setTitle("Tabs")
            .setItems(tabs.toTypedArray()) { _, which ->
                loadUrl(tabs[which])
            }
            .setPositiveButton("New Tab") { _, _ ->
                tabManager.addTab(currentUrl)
                loadUrl(prefsManager.getHomepage())
                updateTabCount()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun updateTabCount() {
        val count = tabManager.getTabs().size
        tvTabCount.text = count.toString()
        tvTabCount.visibility = if (count > 1) View.VISIBLE else View.GONE
    }

    private fun showBookmarks() {
        val bookmarks = prefsManager.getBookmarks()
        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Bookmarks")
                .setMessage("No bookmarks yet")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setItems(bookmarks.map { it.first }.toTypedArray()) { _, which ->
                loadUrl(bookmarks[which].second)
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showHistory() {
        val history = prefsManager.getHistory()
        if (history.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("History")
                .setMessage("No history yet")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("History")
            .setItems(history.takeLast(20).reversed().toTypedArray()) { _, which ->
                loadUrl(history.takeLast(20).reversed()[which])
            }
            .setPositiveButton("Clear History") { _, _ ->
                prefsManager.clearHistory()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun sharePage() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, webView.url)
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        bannerAdView?.resume()
        isAdBlockEnabled = prefsManager.isAdBlockEnabled()
        updateTabCount()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        bannerAdView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAdView?.destroy()
        webView.destroy()
        adManager.destroy()
    }
}
