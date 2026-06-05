package com.litebrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.litebrowser.adblock.AdBlocker
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
    private lateinit var btnMenu: ImageButton

    private lateinit var tabManager: TabManager
    private lateinit var adBlocker: AdBlocker
    private lateinit var prefsManager: PrefsManager

    private var isAdBlockEnabled = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)

        // Initialize managers
        tabManager = TabManager(this)
        adBlocker = AdBlocker(this)
        prefsManager = PrefsManager(this)

        isAdBlockEnabled = prefsManager.isAdBlockEnabled()

        setupWebView()
        setupListeners()

        // Load homepage
        val intentUrl = intent?.data?.toString()
        if (intentUrl != null) {
            loadUrl(intentUrl)
        } else {
            loadUrl(prefsManager.getHomepage())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
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
            userAgentString = prefsManager.getUserAgent()
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

    private fun setupListeners() {
        // URL bar
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = urlBar.text.toString().trim()
                loadUrl(input)
                true
            } else false
        }

        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                urlBar.selectAll()
            }
        }

        // Navigation buttons
        btnBack.setOnClickListener { webView.goBack() }
        btnForward.setOnClickListener { webView.goForward() }

        // Tabs button
        btnTabs.setOnClickListener { showTabsDialog() }

        // Menu button
        btnMenu.setOnClickListener { showMenu() }

        // Swipe refresh
        swipeRefresh.setOnRefreshListener { webView.reload() }
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

        val items = tabs.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tabs")
            .setItems(items) { _, which ->
                loadUrl(tabs[which])
            }
            .setPositiveButton("New Tab") { _, _ ->
                tabManager.addTab(currentUrl)
                loadUrl(prefsManager.getHomepage())
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMenu() {
        val items = arrayOf(
            "New Tab",
            "Bookmarks",
            "History",
            "Share",
            "Find in Page",
            "Desktop Site",
            "Settings"
        )

        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        tabManager.addTab(webView.url ?: "")
                        loadUrl(prefsManager.getHomepage())
                    }
                    1 -> showBookmarks()
                    2 -> showHistory()
                    3 -> sharePage()
                    4 -> showFindInPage()
                    5 -> toggleDesktopSite()
                    6 -> openSettings()
                }
            }
            .show()
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

        val names = bookmarks.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setItems(names) { _, which ->
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

        val items = history.takeLast(20).reversed().toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("History")
            .setItems(items) { _, which ->
                loadUrl(items[which])
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

    private fun showFindInPage() {
        val editText = EditText(this)
        editText.hint = "Find in page"

        AlertDialog.Builder(this)
            .setTitle("Find in Page")
            .setView(editText)
            .setPositiveButton("Find") { _, _ ->
                webView.findAllAsync(editText.text.toString())
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toggleDesktopSite() {
        val currentUrl = webView.url ?: return
        val isDesktop = webView.settings.userAgentString?.contains("Desktop") == true

        if (isDesktop) {
            webView.settings.userAgentString = prefsManager.getUserAgent()
        } else {
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        webView.loadUrl(currentUrl)
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        isAdBlockEnabled = prefsManager.isAdBlockEnabled()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
