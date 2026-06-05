package com.litebrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdView
import com.litebrowser.adblock.AdBlocker
import com.litebrowser.ads.AdManager
import com.litebrowser.browser.TabManager
import com.litebrowser.settings.SettingsActivity
import com.litebrowser.utils.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
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

    private var bannerAdView: AdView? = null

    companion object {
        private const val REQUEST_SETTINGS = 1001
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webViewContainer = findViewById(R.id.webViewContainer)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
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
        prefsManager = PrefsManager(this)
        adBlocker = AdBlocker(this)
        adManager = AdManager(this)

        // Initialize TabManager with callbacks
        tabManager = TabManager(
            context = this,
            container = webViewContainer,
            progressBar = progressBar,
            adBlocker = adBlocker,
            onPageStarted = { url -> urlBar.setText(url) },
            onPageFinished = { _ ->
                progressBar.visibility = View.GONE
                updateNavigationButtons()
                adManager.onPageLoaded()
            },
            onTitleReceived = { title ->
                supportActionBar?.title = title ?: getString(R.string.app_name)
            },
            onProgressChanged = { progress ->
                progressBar.progress = progress
            }
        )

        // Apply settings
        tabManager.setAdBlockEnabled(prefsManager.isAdBlockEnabled())
        tabManager.setDesktopMode(prefsManager.isDesktopMode())
        tabManager.setDarkMode(prefsManager.getDarkMode())

        setupListeners()
        setupAds()

        // Create first tab
        val firstTab = tabManager.createTab()
        tabManager.switchToTab(0)

        // Load URL
        val intentUrl = intent?.data?.toString()
        if (intentUrl != null) {
            tabManager.loadUrl(intentUrl)
        } else {
            tabManager.loadUrl(prefsManager.getHomepage())
        }

        updateTabCount()
    }

    private fun setupAds() {
        adManager.initialize()
        bannerAdView = adManager.createBannerAd()
        adContainer.addView(bannerAdView)
        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        bannerAdView?.loadAd(adRequest)
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

        btnBack.setOnClickListener {
            if (tabManager.canGoBack()) tabManager.goBack()
        }
        btnForward.setOnClickListener {
            if (tabManager.canGoForward()) tabManager.goForward()
        }
        btnTabs.setOnClickListener { showTabsDialog() }

        btnHome.setOnClickListener { tabManager.loadUrl(prefsManager.getHomepage()) }
        btnBookmarks.setOnClickListener { showBookmarks() }
        btnHistory.setOnClickListener { showHistory() }
        btnShare.setOnClickListener { sharePage() }
        btnSettings.setOnClickListener {
            startActivityForResult(Intent(this, SettingsActivity::class.java), REQUEST_SETTINGS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            // Apply settings changes
            tabManager.setAdBlockEnabled(prefsManager.isAdBlockEnabled())
            tabManager.setDesktopMode(prefsManager.isDesktopMode())
            tabManager.setDarkMode(prefsManager.getDarkMode())

            // Reload current tab
            tabManager.reload()
        }
    }

    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${Uri.encode(input)}"
        }
        tabManager.loadUrl(url)
        urlBar.setText(url)
    }

    private fun updateNavigationButtons() {
        btnBack.alpha = if (tabManager.canGoBack()) 1.0f else 0.3f
        btnForward.alpha = if (tabManager.canGoForward()) 1.0f else 0.3f
    }

    private fun showTabsDialog() {
        val tabs = tabManager.getTabs()
        val currentTabIndex = tabManager.getActiveTabIndex()

        // Build tab list with titles
        val tabTitles = tabs.mapIndexed { index, tab ->
            val prefix = if (index == currentTabIndex) "▶ " else "   "
            "$prefix${tab.title.take(30)}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Tabs (${tabs.size})")
            .setItems(tabTitles) { _, which ->
                tabManager.switchToTab(which)
                updateTabCount()
                updateNavigationButtons()
                val url = tabManager.getCurrentUrl() ?: ""
                if (url.isNotEmpty()) urlBar.setText(url)
            }
            .setPositiveButton("+ New Tab") { _, _ ->
                tabManager.createTab()
                val newIndex = tabManager.getTabCount() - 1
                tabManager.switchToTab(newIndex)
                tabManager.loadUrl(prefsManager.getHomepage())
                urlBar.setText(prefsManager.getHomepage())
                updateTabCount()
            }
            .setNeutralButton("Close Tab") { _, _ ->
                showCloseTabDialog()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCloseTabDialog() {
        val tabs = tabManager.getTabs()
        if (tabs.size <= 1) {
            Toast.makeText(this, "Can't close the last tab", Toast.LENGTH_SHORT).show()
            return
        }

        val tabTitles = tabs.map { it.title.take(30) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Close Tab")
            .setItems(tabTitles) { _, which ->
                tabManager.closeTab(which)
                updateTabCount()
                val url = tabManager.getCurrentUrl() ?: ""
                if (url.isNotEmpty()) urlBar.setText(url)
                updateNavigationButtons()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTabCount() {
        val count = tabManager.getTabCount()
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, tabManager.getCurrentUrl())
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (tabManager.canGoBack()) tabManager.goBack()
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        tabManager.onResume()
        bannerAdView?.resume()
        tabManager.setAdBlockEnabled(prefsManager.isAdBlockEnabled())
        updateTabCount()
    }

    override fun onPause() {
        super.onPause()
        tabManager.onPause()
        bannerAdView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAdView?.destroy()
        tabManager.destroy()
        adManager.destroy()
    }
}
