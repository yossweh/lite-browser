package com.litebrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.litebrowser.data.AppDatabase
import com.litebrowser.data.Bookmark
import com.litebrowser.data.TabInfo
import com.litebrowser.helper.AdBlocker
import com.litebrowser.helper.AdManager
import com.litebrowser.helper.PrefManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class BrowserActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_DOWNLOAD = 1001
        private const val INTERSTITIAL_PAGE_THRESHOLD = 15
        private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // Tab management
    private val tabs = HashMap<String, WebView>()
    private val tabInfoList = mutableListOf<TabInfo>()
    private var activeTabId = ""

    // UI
    private lateinit var urlBar: EditText
    private lateinit var goButton: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var webContainer: FrameLayout
    private lateinit var btnBack: View
    private lateinit var btnForward: View
    private lateinit var btnShare: View
    private lateinit var btnTabs: View
    private lateinit var btnMenu: View
    private lateinit var tabCountBadge: TextView
    private lateinit var adContainer: FrameLayout

    // AdMob
    private var pageLoadCount = 0
    private var lastInterstitialTime = 0L
    private var rewardedUntil = 0L // timestamp until which interstitials are disabled

    // Download pending
    private var pendingUrl = ""
    private var pendingDisposition = ""
    private var pendingMime = ""

    private val scope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        initViews()
        setupClickListeners()
        setupAdMob()

        val intentUrl = handleIncomingIntent(intent)
        val startUrl = intentUrl ?: PrefManager.homepage.ifEmpty { "https://www.google.com" }
        newTab(startUrl)
    }

    private fun initViews() {
        urlBar = findViewById(R.id.url_bar)
        goButton = findViewById(R.id.btn_go)
        btnRefresh = findViewById(R.id.btn_refresh)
        progressBar = findViewById(R.id.progress_bar)
        webContainer = findViewById(R.id.web_container)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnShare = findViewById(R.id.btn_share)
        btnTabs = findViewById(R.id.btn_tabs)
        btnMenu = findViewById(R.id.btn_menu)
        tabCountBadge = findViewById(R.id.tab_count_badge)
        adContainer = findViewById(R.id.ad_container)
    }

    private fun setupClickListeners() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUrlFromBar()
                true
            } else false
        }

        urlBar.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) urlBar.selectAll() }

        goButton.setOnClickListener { loadUrlFromBar() }
        btnRefresh.setOnClickListener { getCurrentWebView()?.reload() }

        btnBack.setOnClickListener { getCurrentWebView()?.goBack() }
        btnForward.setOnClickListener { getCurrentWebView()?.goForward() }
        btnShare.setOnClickListener {
            getCurrentWebView()?.url?.let { url ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share URL"))
            }
        }
        btnTabs.setOnClickListener { showTabsDialog() }
        btnMenu.setOnClickListener { showPopupMenu(it) }
    }

    // ==================== TAB MANAGEMENT ====================

    @SuppressLint("SetJavaScriptEnabled")
    fun newTab(url: String) {
        val tabId = UUID.randomUUID().toString()

        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                setSupportMultipleWindows(false)
            }

            // Apply desktop mode default
            if (PrefManager.desktopModeDefault) {
                settings.userAgentString = DESKTOP_USER_AGENT
            }

            webViewClient = createWebViewClient()
            webChromeClient = createWebChromeClient()
            setDownloadListener(handleDownload)

            // Register for context menu
            registerForContextMenu(this)
        }

        // Apply dark mode
        applyDarkMode(webView)

        tabs[tabId] = webView
        tabInfoList.add(TabInfo(tabId, "New Tab", url, PrefManager.desktopModeDefault))
        switchTab(tabId)
        webView.loadUrl(url)
    }

    fun switchTab(tabId: String) {
        val webView = tabs[tabId] ?: return

        // Hide all WebViews
        for (i in 0 until webContainer.childCount) {
            webContainer.getChildAt(i).visibility = View.GONE
        }

        // Show or add current WebView
        if (webView.parent == null) {
            webContainer.addView(webView)
        }
        webView.visibility = View.VISIBLE

        activeTabId = tabId
        updateUrlBar(webView.url ?: "")
        updateTabCount()
    }

    fun closeTab(tabId: String) {
        if (tabs.size <= 1) {
            // Don't close last tab, create new one instead
            tabs[tabId]?.loadUrl(PrefManager.homepage)
            return
        }

        val webView = tabs.remove(tabId)
        webView?.destroy()
        tabInfoList.removeAll { it.id == tabId }

        if (activeTabId == tabId) {
            val newActive = tabs.keys.firstOrNull() ?: return
            switchTab(newActive)
        }
        updateTabCount()
    }

    fun getCurrentWebView(): WebView? = tabs[activeTabId]

    private fun updateUrlBar(url: String) {
        if (!urlBar.hasFocus()) {
            urlBar.setText(url)
        }
    }

    private fun updateTabCount() {
        tabCountBadge.text = tabs.size.toString()
    }

    // ==================== WEBVIEW CLIENTS ====================

    private fun createWebViewClient() = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null
            if (PrefManager.adBlockEnabled && AdBlocker.shouldBlock(url)) {
                return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { updateUrlBar(it) }
            progressBar.visibility = View.VISIBLE

            // Update tab info
            tabInfoList.find { it.id == activeTabId }?.url = url ?: ""
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE

            // Apply dark mode CSS if enabled
            if (PrefManager.darkMode) {
                view?.let { injectDarkModeCSS(it, true) }
            }

            // Update tab title
            tabInfoList.find { it.id == activeTabId }?.apply {
                title = view?.title ?: "New Tab"
                this.url = url ?: ""
            }

            // Interstitial ad logic (skip if rewarded premium active)
            pageLoadCount++
            val now = System.currentTimeMillis()
            if (now > rewardedUntil && // not in reward period
                pageLoadCount % INTERSTITIAL_PAGE_THRESHOLD == 0 &&
                (lastInterstitialTime == 0L || now - lastInterstitialTime > INTERSTITIAL_COOLDOWN_MS)
            ) {
                AdManager.showInterstitial(this@BrowserActivity)
                lastInterstitialTime = now
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString() ?: return false

            // Handle special schemes
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
                return true
            }
            return false
        }
    }

    private fun createWebChromeClient() = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            progressBar.progress = newProgress
            if (newProgress >= 100) {
                progressBar.visibility = View.GONE
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            tabInfoList.find { it.id == activeTabId }?.title = title ?: "New Tab"
        }
    }

    // ==================== URL LOADING ====================

    private fun loadUrlFromBar() {
        val input = urlBar.text.toString().trim()
        if (input.isEmpty()) return

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
        urlBar.clearFocus()

        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> PrefManager.searchEngine + Uri.encode(input)
        }

        getCurrentWebView()?.loadUrl(url)
    }

    // ==================== DOWNLOADS ====================

    private val handleDownload = DownloadListener { url, _, contentDisposition, mimeType, _ ->
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            pendingUrl = url
            pendingDisposition = contentDisposition
            pendingMime = mimeType
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_DOWNLOAD
            )
        } else {
            startDownload(url, contentDisposition, mimeType)
        }
    }

    private fun startDownload(url: String, contentDisposition: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                setMimeType(mimeType)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                addRequestHeader("User-Agent", getCurrentWebView()?.settings?.userAgentString)
                setDescription("Downloading file...")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_DOWNLOAD && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startDownload(pendingUrl, pendingDisposition, pendingMime)
        }
    }

    // ==================== POPUP MENU ====================

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_browser, popup.menu)

        popup.menu.findItem(R.id.action_desktop_mode)?.isChecked =
            tabInfoList.find { it.id == activeTabId }?.isDesktopMode ?: false
        popup.menu.findItem(R.id.action_dark_mode)?.isChecked = PrefManager.darkMode

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> getCurrentWebView()?.reload()
                R.id.action_new_tab -> {
                    newTab(PrefManager.homepage.ifEmpty { "https://www.google.com" })
                }
                R.id.action_bookmarks -> showBookmarksDialog()
                R.id.action_add_bookmark -> addCurrentBookmark()
                R.id.action_desktop_mode -> toggleDesktopMode()
                R.id.action_dark_mode -> toggleDarkMode()
                R.id.action_watch_ad -> showRewardedAdForPremium()
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.action_exit -> finish()
            }
            true
        }
        popup.show()
    }

    // ==================== TABS DIALOG ====================

    private fun showTabsDialog() {
        val builder = AlertDialog.Builder(this, R.style.Theme_LiteBrowser)
        val view = layoutInflater.inflate(R.layout.dialog_tabs, null)
        builder.setView(view)

        val dialog = builder.create()

        // Using LinearLayout for tabs
        val btnNewTab = view.findViewById<View>(R.id.btn_new_tab)

        // Simple tab list using LinearLayout
        val container = view.findViewById<android.widget.LinearLayout>(R.id.tab_list_container_simple)
        container?.removeAllViews()

        for (tabInfo in tabInfoList) {
            val tabView = layoutInflater.inflate(R.layout.item_tab, container, false)
            val title = tabView.findViewById<TextView>(R.id.tab_title)
            val url = tabView.findViewById<TextView>(R.id.tab_url)
            val closeBtn = tabView.findViewById<ImageButton>(R.id.tab_close)

            title.text = tabInfo.title
            url.text = tabInfo.url
            tabView.alpha = if (tabInfo.id == activeTabId) 1.0f else 0.7f

            tabView.setOnClickListener {
                switchTab(tabInfo.id)
                dialog.dismiss()
            }

            closeBtn.setOnClickListener {
                closeTab(tabInfo.id)
                if (tabs.size <= 1) dialog.dismiss()
                // Refresh the dialog
                container.removeView(tabView)
            }

            container?.addView(tabView)
        }

        btnNewTab?.setOnClickListener {
            newTab(PrefManager.homepage.ifEmpty { "https://www.google.com" })
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==================== BOOKMARKS ====================

    private fun addCurrentBookmark() {
        val webView = getCurrentWebView() ?: return
        val title = webView.title ?: "Bookmark"
        val url = webView.url ?: return

        scope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(this@BrowserActivity).bookmarkDao()
                .insert(Bookmark(title = title, url = url, createdAt = System.currentTimeMillis()))
            launch(Dispatchers.Main) {
                Toast.makeText(this@BrowserActivity, "Bookmark added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBookmarksDialog() {
        scope.launch(Dispatchers.IO) {
            val bookmarks = AppDatabase.getInstance(this@BrowserActivity).bookmarkDao().getAll()
            launch(Dispatchers.Main) {
                if (bookmarks.isEmpty()) {
                    Toast.makeText(this@BrowserActivity, "No bookmarks", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val names = bookmarks.map { it.title }.toTypedArray()
                AlertDialog.Builder(this@BrowserActivity)
                    .setTitle("Bookmarks")
                    .setItems(names) { _, which ->
                        getCurrentWebView()?.loadUrl(bookmarks[which].url)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    // ==================== DESKTOP MODE ====================

    private fun toggleDesktopMode() {
        val tabInfo = tabInfoList.find { it.id == activeTabId } ?: return
        tabInfo.isDesktopMode = !tabInfo.isDesktopMode

        val webView = getCurrentWebView() ?: return
        if (tabInfo.isDesktopMode) {
            webView.settings.userAgentString = DESKTOP_USER_AGENT
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        } else {
            // Reset to default mobile UA
            webView.settings.userAgentString = WebSettings.getDefaultUserAgent(this)
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        }
        webView.reload()

        Toast.makeText(
            this,
            if (tabInfo.isDesktopMode) "Desktop mode ON" else "Desktop mode OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ==================== REWARDED AD (FREE PREMIUM) ====================

    private fun showRewardedAdForPremium() {
        val remaining = rewardedUntil - System.currentTimeMillis()
        if (remaining > 0) {
            val mins = (remaining / 60000).toInt()
            Toast.makeText(this, "🎁 Premium active! ${mins}min remaining", Toast.LENGTH_LONG).show()
            return
        }

        AdManager.showRewarded(this) {
            // Reward: 30 minutes without interstitial ads
            rewardedUntil = System.currentTimeMillis() + 30 * 60 * 1000L
            Toast.makeText(this, "🎁 Premium active! No ads for 30 minutes", Toast.LENGTH_LONG).show()
            // Pre-load next rewarded ad
            AdManager.loadRewarded(this)
        }
    }

    // ==================== DARK MODE ====================

    private fun toggleDarkMode() {
        PrefManager.darkMode = !PrefManager.darkMode
        applyDarkMode(getCurrentWebView())

        Toast.makeText(
            this,
            if (PrefManager.darkMode) "Dark mode ON" else "Dark mode OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyDarkMode(webView: WebView?) {
        webView ?: return
        if (PrefManager.darkMode) {
            // Try native force dark first
            if (Build.VERSION.SDK_INT >= 33) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
                }
            } else if (Build.VERSION.SDK_INT >= 29) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                }
            }
            // Inject dark mode CSS as fallback
            injectDarkModeCSS(webView, true)
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
                }
            } else if (Build.VERSION.SDK_INT >= 29) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }
            injectDarkModeCSS(webView, false)
        }
    }

    private fun injectDarkModeCSS(webView: WebView, enable: Boolean) {
        if (enable) {
            val css = """
                (function() {
                    var style = document.getElementById('litebrowser-dark-css');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'litebrowser-dark-css';
                        style.textContent = `
                            html { filter: invert(85%) hue-rotate(180deg) !important; }
                            img, video, iframe, canvas, svg, [style*="background-image"] {
                                filter: invert(100%) hue-rotate(180deg) !important;
                            }
                        `;
                        document.head.appendChild(style);
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(css, null)
        } else {
            val css = """
                (function() {
                    var style = document.getElementById('litebrowser-dark-css');
                    if (style) style.remove();
                })();
            """.trimIndent()
            webView.evaluateJavascript(css, null)
        }
    }

    // ==================== CONTEXT MENU ====================

    override fun onCreateContextMenu(
        menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = v as? WebView ?: return
        val hitTestResult = webView.hitTestResult

        when (hitTestResult.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                menu?.setHeaderTitle("Link")
                menu?.add(0, 1, 0, "Open in new tab")
                menu?.add(0, 2, 0, "Copy link")
                menu?.add(0, 3, 0, "Share link")
            }
            WebView.HitTestResult.IMAGE_TYPE -> {
                menu?.setHeaderTitle("Image")
                menu?.add(0, 4, 0, "Open image")
                menu?.add(0, 5, 0, "Copy image URL")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val webView = getCurrentWebView() ?: return super.onContextItemSelected(item)
        val result = webView.hitTestResult
        val url = result.extra ?: return super.onContextItemSelected(item)

        when (item.itemId) {
            1 -> newTab(url)
            2 -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
            3 -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }
            4 -> newTab(url)
            5 -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Image URL", url))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    // ==================== ADMOB ====================

    private fun setupAdMob() {
        // Banner ad
        val adView = AdView(this).apply {
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            setAdSize(AdSize.BANNER)
        }
        adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())

        // Load interstitial
        AdManager.loadInterstitial(this)
        
        // Load rewarded ad
        AdManager.loadRewarded(this)
    }

    // ==================== INTENT HANDLING ====================

    private fun handleIncomingIntent(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_VIEW) {
            return intent.data?.toString()
        }
        return null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val url = handleIncomingIntent(intent)
        if (url != null) {
            newTab(url)
        }
    }

    // ==================== BACK PRESS ====================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView = getCurrentWebView()
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else if (tabs.size > 1) {
            closeTab(activeTabId)
        } else {
            super.onBackPressed()
        }
    }

    // ==================== LIFECYCLE ====================

    override fun onResume() {
        super.onResume()
        getCurrentWebView()?.onResume()
    }

    override fun onPause() {
        super.onPause()
        getCurrentWebView()?.onPause()
    }

    override fun onDestroy() {
        tabs.values.forEach { it.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
