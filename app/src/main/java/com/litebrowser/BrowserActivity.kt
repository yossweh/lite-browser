package com.litebrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.litebrowser.db.AppDatabase
import com.litebrowser.db.Bookmark
import com.litebrowser.helper.AdBlocker
import com.litebrowser.helper.AdManager
import com.litebrowser.helper.PrefManager
import com.litebrowser.model.TabInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class BrowserActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_DOWNLOAD = 1001
        private const val INTERSTITIAL_PAGE_THRESHOLD = 15
        private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // Tab management
    private val tabs = HashMap<String, WebView>()
    private val tabInfoList = mutableListOf<TabInfo>()
    private var activeTabId: String = ""

    // UI elements
    private lateinit var toolbarContainer: View
    private lateinit var urlBar: EditText
    private lateinit var goButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tabContentContainer: FrameLayout
    private lateinit var bottomNavBar: View
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnTabs: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var tabCountBadge: TextView
    private lateinit var adContainer: FrameLayout

    // AdMob
    private var interstitialAd: InterstitialAd? = null
    private var pageLoadCount = 0
    private var lastInterstitialTime = 0L

    // Pending download info
    private var pendingDownloadUrl: String = ""
    private var pendingDownloadContentDisposition: String = ""
    private var pendingDownloadMimeType: String = ""

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        initViews()
        setupClickListeners()
        loadInterstitialAd()
        loadBannerAd()

        // Handle incoming VIEW intent
        val intentUrl = handleIncomingIntent(intent)

        // Create initial tab
        val startUrl = intentUrl ?: PrefManager.homepage.ifEmpty { "https://www.google.com" }
        newTab(startUrl)
    }

    private fun initViews() {
        toolbarContainer = findViewById(R.id.toolbar_container)
        urlBar = findViewById(R.id.url_bar)
        goButton = findViewById(R.id.btn_go)
        progressBar = findViewById(R.id.progress_bar)
        tabContentContainer = findViewById(R.id.tab_content_container)
        bottomNavBar = findViewById(R.id.bottom_nav_bar)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnShare = findViewById(R.id.btn_share)
        btnTabs = findViewById(R.id.btn_tabs)
        btnMenu = findViewById(R.id.btn_menu)
        tabCountBadge = findViewById(R.id.tab_count_badge)
        adContainer = findViewById(R.id.ad_container)
    }

    private fun setupClickListeners() {
        // URL bar - handle enter key and focus
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUrlFromBar()
                true
            } else false
        }

        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                urlBar.selectAll()
            }
        }

        goButton.setOnClickListener {
            loadUrlFromBar()
        }

        btnBack.setOnClickListener {
            getCurrentWebView()?.let { webView ->
                if (webView.canGoBack()) webView.goBack()
            }
        }

        btnForward.setOnClickListener {
            getCurrentWebView()?.let { webView ->
                if (webView.canGoForward()) webView.goForward()
            }
        }

        btnShare.setOnClickListener {
            getCurrentWebView()?.url?.let { url ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share URL"))
            }
        }

        btnTabs.setOnClickListener {
            showTabsDialog()
        }

        btnMenu.setOnClickListener { anchor ->
            showPopupMenu(anchor)
        }
    }

    private fun loadUrlFromBar() {
        val input = urlBar.text.toString().trim()
        if (input.isEmpty()) return

        val url = processInput(input)
        getCurrentWebView()?.loadUrl(url)

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
        urlBar.clearFocus()
    }

    private fun processInput(input: String): String {
        // Already a full URL
        if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("file://")) {
            return input
        }
        // Looks like a domain (contains a dot, no spaces)
        if (input.contains(".") && !input.contains(" ")) {
            return "https://$input"
        }
        // Treat as search query
        val searchEngine = PrefManager.searchEngine
        val searchUrl = when (searchEngine) {
            "google" -> "https://www.google.com/search?q="
            "duckduckgo" -> "https://duckduckgo.com/?q="
            "bing" -> "https://www.bing.com/search?q="
            "yahoo" -> "https://search.yahoo.com/search?p="
            else -> "https://www.google.com/search?q="
        }
        return searchUrl + Uri.encode(input)
    }

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

    // ======================== Tab Management ========================

    @SuppressLint("SetJavaScriptEnabled")
    fun newTab(url: String) {
        val tabId = UUID.randomUUID().toString()

        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        setupWebView(webView, tabId)

        tabs[tabId] = webView
        tabInfoList.add(TabInfo(id = tabId, title = "New Tab", url = url, isDesktopMode = PrefManager.desktopModeDefault))

        // Add to container but hide it
        webView.visibility = View.GONE
        tabContentContainer.addView(webView)

        // Switch to the new tab
        switchTab(tabId)

        // Load the URL
        webView.loadUrl(url)

        updateTabCountBadge()
    }

    fun switchTab(tabId: String) {
        val webView = tabs[tabId] ?: return

        // Hide current WebView
        tabs[activeTabId]?.visibility = View.GONE

        // Show new WebView
        webView.visibility = View.VISIBLE
        activeTabId = tabId

        // Update URL bar
        val info = tabInfoList.find { it.id == tabId }
        urlBar.setText(webView.url ?: info?.url ?: "")

        // Update desktop mode state
        updateDesktopModeUI(webView)

        updateTabCountBadge()
    }

    fun closeTab(tabId: String) {
        val webView = tabs.remove(tabId) ?: return
        tabInfoList.removeAll { it.id == tabId }

        webView.stopLoading()
        webView.destroy()
        tabContentContainer.removeView(webView)

        if (tabInfoList.isEmpty()) {
            // Create a new tab if all tabs closed
            newTab(PrefManager.homepage.ifEmpty { "https://www.google.com" })
        } else if (activeTabId == tabId) {
            // Switch to the last tab
            switchTab(tabInfoList.last().id)
        }

        updateTabCountBadge()
    }

    fun getCurrentWebView(): WebView? {
        return tabs[activeTabId]
    }

    private fun updateTabCountBadge() {
        tabCountBadge.text = tabs.size.toString()
        tabCountBadge.visibility = if (tabs.size > 0) View.VISIBLE else View.GONE
    }

    private fun showTabsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tabs, null)
        val tabListContainer = dialogView.findViewById<ViewGroup>(R.id.tab_list_container)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tabs (${tabs.size})")
            .setView(dialogView)
            .setPositiveButton("New Tab") { _, _ ->
                newTab(PrefManager.homepage.ifEmpty { "https://www.google.com" })
            }
            .setNegativeButton("Close", null)
            .create()

        // Populate tab list
        for (info in tabInfoList) {
            val tabView = LayoutInflater.from(this).inflate(R.layout.item_tab, tabListContainer, false)
            val titleText = tabView.findViewById<TextView>(R.id.tab_title)
            val urlText = tabView.findViewById<TextView>(R.id.tab_url)
            val closeBtn = tabView.findViewById<ImageButton>(R.id.tab_close)

            titleText.text = info.title
            urlText.text = info.url

            // Highlight active tab
            if (info.id == activeTabId) {
                tabView.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active_bg))
            }

            tabView.setOnClickListener {
                switchTab(info.id)
                dialog.dismiss()
            }

            closeBtn.setOnClickListener {
                closeTab(info.id)
                // Refresh the dialog
                tabListContainer.removeView(tabView)
                if (tabs.isEmpty()) dialog.dismiss()
            }

            tabListContainer.addView(tabView)
        }

        dialog.show()
    }

    // ======================== WebView Setup ========================

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, tabId: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)

            // Apply desktop mode if needed
            val info = tabInfoList.find { it.id == tabId }
            if (info?.isDesktopMode == true) {
                userAgentString = DESKTOP_USER_AGENT
            }
        }

        // Cookie manager
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        // Apply dark mode
        applyDarkMode(webView)

        // WebViewClient
        webView.webViewClient = object : WebViewClient() {
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
                if (view?.id == tabs[activeTabId]?.id) {
                    progressBar.visibility = View.VISIBLE
                    urlBar.setText(url ?: "")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view?.id == tabs[activeTabId]?.id) {
                    progressBar.visibility = View.GONE
                    urlBar.setText(url ?: "")
                }
                // Update tab info
                val info = tabInfoList.find { it.id == tabId }
                info?.url = url ?: ""
                info?.title = view?.title ?: "Untitled"

                // Interstitial ad logic
                pageLoadCount++
                val now = System.currentTimeMillis()
                if (pageLoadCount >= INTERSTITIAL_PAGE_THRESHOLD &&
                    (now - lastInterstitialTime) >= INTERSTITIAL_COOLDOWN_MS
                ) {
                    showInterstitialAd()
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
                    } catch (e: Exception) {
                        // Ignore if no app handles it
                    }
                    return true
                }
                return false
            }
        }

        // WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val info = tabInfoList.find { it.id == tabId }
                info?.title = title ?: "Untitled"
                if (view?.id == tabs[activeTabId]?.id) {
                    urlBar.setText(view.url ?: "")
                }
            }
        }

        // Download listener
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownloadUrl = url
                pendingDownloadContentDisposition = contentDisposition
                pendingDownloadMimeType = mimetype
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_DOWNLOAD
                )
            } else {
                startDownload(url, contentDisposition, mimetype)
            }
        }

        // Long press context menu
        registerForContextMenu(webView)
    }

    // ======================== Context Menu ========================

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v is WebView) {
            val hitTestResult = v.hitTestResult
            when (hitTestResult.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    menu?.setHeaderTitle("Link")
                    menu?.add(0, 1, 0, "Open in new tab")
                    menu?.add(0, 2, 0, "Copy link")
                    menu?.add(0, 3, 0, "Share link")
                }
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                WebView.HitTestResult.IMAGE_TYPE -> {
                    menu?.setHeaderTitle("Image")
                    menu?.add(0, 4, 0, "Open image in new tab")
                    menu?.add(0, 5, 0, "Save image")
                    menu?.add(0, 6, 0, "Copy image link")
                    menu?.add(0, 7, 0, "Share image link")
                }
                WebView.HitTestResult.PHONE_TYPE -> {
                    menu?.setHeaderTitle("Phone")
                    menu?.add(0, 8, 0, "Call number")
                    menu?.add(0, 9, 0, "Copy number")
                }
                WebView.HitTestResult.EMAIL_TYPE -> {
                    menu?.setHeaderTitle("Email")
                    menu?.add(0, 10, 0, "Send email")
                    menu?.add(0, 11, 0, "Copy email")
                }
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val webView = getCurrentWebView() ?: return super.onContextItemSelected(item)
        val result = webView.hitTestResult
        val extra = result.extra ?: return super.onContextItemSelected(item)

        when (item.itemId) {
            1 -> newTab(extra) // Open link in new tab
            2 -> { // Copy link
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", extra))
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show()
            }
            3 -> { // Share link
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, extra)
                }
                startActivity(Intent.createChooser(shareIntent, "Share link"))
            }
            4 -> newTab(extra) // Open image in new tab
            5 -> { // Save image
                startDownload(extra, "", "image/*")
            }
            6 -> { // Copy image link
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Image URL", extra))
                Toast.makeText(this, "Image link copied", Toast.LENGTH_SHORT).show()
            }
            7 -> { // Share image link
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, extra)
                }
                startActivity(Intent.createChooser(shareIntent, "Share image link"))
            }
            8 -> { // Call number
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$extra")))
            }
            9 -> { // Copy number
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Phone", extra))
                Toast.makeText(this, "Number copied", Toast.LENGTH_SHORT).show()
            }
            10 -> { // Send email
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$extra")))
            }
            11 -> { // Copy email
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Email", extra))
                Toast.makeText(this, "Email copied", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    // ======================== Menu ========================

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor, Gravity.END)
        popup.menuInflater.inflate(R.menu.menu_browser, popup.menu)

        // Update toggle states
        popup.menu.findItem(R.id.action_desktop_mode)?.isChecked = PrefManager.desktopModeDefault
        popup.menu.findItem(R.id.action_dark_mode)?.isChecked = PrefManager.darkMode

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_new_tab -> {
                    newTab(PrefManager.homepage.ifEmpty { "https://www.google.com" })
                    true
                }
                R.id.action_bookmarks -> {
                    showBookmarksDialog()
                    true
                }
                R.id.action_add_bookmark -> {
                    addCurrentBookmark()
                    true
                }
                R.id.action_desktop_mode -> {
                    toggleDesktopMode()
                    true
                }
                R.id.action_dark_mode -> {
                    toggleDarkMode()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_exit -> {
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // ======================== Desktop Mode ========================

    private fun toggleDesktopMode() {
        val webView = getCurrentWebView() ?: return
        val tabInfo = tabInfoList.find { it.id == activeTabId } ?: return
        val newDesktopMode = !tabInfo.isDesktopMode
        tabInfo.isDesktopMode = newDesktopMode

        webView.settings.userAgentString = if (newDesktopMode) DESKTOP_USER_AGENT else null
        webView.reload()
        updateDesktopModeUI(webView)
    }

    private fun updateDesktopModeUI(webView: WebView) {
        // Could update a visual indicator if needed
    }

    // ======================== Dark Mode ========================

    private fun toggleDarkMode() {
        PrefManager.darkMode = !PrefManager.darkMode
        tabs.values.forEach { webView -> applyDarkMode(webView) }
        // Reload current tab to apply
        getCurrentWebView()?.reload()
    }

    @Suppress("DEPRECATION")
    private fun applyDarkMode(webView: WebView) {
        if (PrefManager.darkMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
                }
            } else {
                // API < 33
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        WebSettingsCompat.FORCE_DARK_AUTO
                    )
                }
            }
            // Also try to inject dark CSS for better support
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
                }
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            }
        }
    }

    // ======================== Bookmarks ========================

    private fun showBookmarksDialog() {
        val db = AppDatabase.getInstance(this)
        val dao = db.bookmarkDao()

        scope.launch {
            dao.getAll().collectLatest { bookmarks ->
                val names = bookmarks.map { it.title }.toTypedArray()
                val dialog = androidx.appcompat.app.AlertDialog.Builder(this@BrowserActivity)
                    .setTitle("Bookmarks")
                    .setItems(names) { _, which ->
                        val bookmark = bookmarks[which]
                        getCurrentWebView()?.loadUrl(bookmark.url)
                    }
                    .setNeutralButton("Delete") { _, _ ->
                        // Show delete selection
                        showDeleteBookmarkDialog(bookmarks)
                    }
                    .setNegativeButton("Close", null)
                    .create()
                dialog.show()
            }
        }
    }

    private fun showDeleteBookmarkDialog(bookmarks: List<Bookmark>) {
        if (bookmarks.isEmpty()) return
        val names = bookmarks.map { it.title }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Bookmark")
            .setItems(names) { _, which ->
                val db = AppDatabase.getInstance(this)
                scope.launch(Dispatchers.IO) {
                    db.bookmarkDao().delete(bookmarks[which])
                }
                Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCurrentBookmark() {
        val webView = getCurrentWebView() ?: return
        val url = webView.url ?: return
        val title = webView.title ?: url

        val bookmark = Bookmark(title = title, url = url)
        val db = AppDatabase.getInstance(this)
        scope.launch(Dispatchers.IO) {
            db.bookmarkDao().insert(bookmark)
        }
        Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
    }

    // ======================== Downloads ========================

    private fun startDownload(url: String, contentDisposition: String, mimeType: String) {
        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                setTitle(fileName)
                setDescription("Downloading $fileName")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                // Forward cookies
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null) {
                    addRequestHeader("Cookie", cookies)
                }
            }
            downloadManager.enqueue(request)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_DOWNLOAD) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload(pendingDownloadUrl, pendingDownloadContentDisposition, pendingDownloadMimeType)
            } else {
                Toast.makeText(this, "Download permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ======================== AdMob ========================

    private fun loadBannerAd() {
        try {
            val adView = AdView(this).apply {
                adSize = com.google.android.gms.ads.AdSize.BANNER
                adUnitId = AdManager.bannerAdUnitId
            }
            adContainer.removeAllViews()
            adContainer.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            // Ad loading failed silently
        }
    }

    private fun loadInterstitialAd() {
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                this,
                AdManager.interstitialAdUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (e: Exception) {
            // Ad loading failed silently
        }
    }

    private fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            ad.show(this)
            pageLoadCount = 0
            lastInterstitialTime = System.currentTimeMillis()
            interstitialAd = null
            // Preload next interstitial
            loadInterstitialAd()
        }
    }

    // ======================== Back Press ========================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView = getCurrentWebView()
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            // If there are multiple tabs, close the current one
            if (tabs.size > 1) {
                closeTab(activeTabId)
            } else {
                super.onBackPressed()
            }
        }
    }

    // ======================== Lifecycle ========================

    override fun onResume() {
        super.onResume()
        tabs.values.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        tabs.values.forEach { it.onPause() }
    }

    override fun onDestroy() {
        // Clean up all WebViews
        tabs.values.forEach { webView ->
            webView.stopLoading()
            webView.destroy()
        }
        tabs.clear()
        tabInfoList.clear()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes without recreating
    }
}
