package com.litebrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager

    private lateinit var tvHomepageValue: TextView
    private lateinit var tvSearchEngineValue: TextView
    private lateinit var switchAdBlocker: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var switchDesktopMode: Switch
    private lateinit var tvVersion: TextView

    private val searchEngines = arrayOf("Google", "DuckDuckGo", "Bing", "Yahoo")
    private val searchEngineUrls = arrayOf(
        "https://www.google.com/search?q=",
        "https://duckduckgo.com/?q=",
        "https://www.bing.com/search?q=",
        "https://search.yahoo.com/search?p="
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)

        if (prefManager.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Initialize views
        tvHomepageValue = findViewById(R.id.tv_homepage_value)
        tvSearchEngineValue = findViewById(R.id.tv_search_engine_value)
        switchAdBlocker = findViewById(R.id.switch_ad_blocker)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        switchDesktopMode = findViewById(R.id.switch_desktop_mode)
        tvVersion = findViewById(R.id.tv_version)

        // Load current values
        loadSettings()

        // Homepage click - show edit dialog
        findViewById<android.view.View>(R.id.layout_homepage).setOnClickListener {
            showHomepageDialog()
        }

        // Search Engine click - show selection dialog
        findViewById<android.view.View>(R.id.layout_search_engine).setOnClickListener {
            showSearchEngineDialog()
        }

        // Ad Blocker toggle
        switchAdBlocker.setOnCheckedChangeListener { _, isChecked ->
            prefManager.adBlockEnabled = isChecked
        }

        // Dark Mode toggle
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefManager.darkMode = isChecked
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Desktop Mode toggle
        switchDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            prefManager.desktopModeDefault = isChecked
        }

        // Clear History button
        findViewById<android.view.View>(R.id.btn_clear_history).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all browsing history?")
                .setPositiveButton("Clear") { _, _ ->
                    try {
                        val db = openOrCreateDatabase("litebrowser_history", MODE_PRIVATE, null)
                        db.execSQL("DELETE FROM history")
                        db.close()
                    } catch (_: Exception) {
                        // Database may not exist yet
                    }
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Clear Cookies button
        findViewById<android.view.View>(R.id.btn_clear_cookies).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Cookies")
                .setMessage("Are you sure you want to clear all cookies and site data?")
                .setPositiveButton("Clear") { _, _ ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()
                    Toast.makeText(this, "Cookies cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Version info
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${pInfo.versionName}"
        } catch (_: Exception) {
            tvVersion.text = "Version 1.0.0"
        }

        // GitHub link
        findViewById<android.view.View>(R.id.layout_github).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nicholasgasior/lite-browser"))
            startActivity(intent)
        }
    }

    private fun loadSettings() {
        tvHomepageValue.text = prefManager.homepage
        tvSearchEngineValue.text = getSearchEngineName(prefManager.searchEngine)
        switchAdBlocker.isChecked = prefManager.adBlockEnabled
        switchDarkMode.isChecked = prefManager.darkMode
        switchDesktopMode.isChecked = prefManager.desktopModeDefault
    }

    private fun showHomepageDialog() {
        val editText = EditText(this).apply {
            setText(prefManager.homepage)
            setPadding(64, 32, 64, 16)
            hint = "https://example.com"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Homepage")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        "https://$url"
                    } else {
                        url
                    }
                    prefManager.homepage = finalUrl
                    tvHomepageValue.text = finalUrl
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchEngineDialog() {
        val currentEngine = getSearchEngineName(prefManager.searchEngine)
        val checkedIndex = searchEngines.indexOf(currentEngine).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Search Engine")
            .setSingleChoiceItems(searchEngines, checkedIndex) { dialog, which ->
                prefManager.searchEngine = searchEngineUrls[which]
                tvSearchEngineValue.text = searchEngines[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getSearchEngineName(url: String?): String {
        return when (url) {
            searchEngineUrls[0] -> searchEngines[0]
            searchEngineUrls[1] -> searchEngines[1]
            searchEngineUrls[2] -> searchEngines[2]
            searchEngineUrls[3] -> searchEngines[3]
            else -> "Google"
        }
    }
}
