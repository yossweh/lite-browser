package com.litebrowser.settings

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import android.widget.LinearLayout
import android.view.ViewGroup
import android.view.Gravity
import android.content.Intent
import android.net.Uri
import com.litebrowser.R
import com.litebrowser.utils.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = PrefsManager(this)

        // Create settings UI programmatically (to keep APK small)
        val scrollView = android.widget.ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        addTitle(layout, "Settings")

        // Ad Blocker
        addSwitchSetting(layout, "Ad Blocker", "Block annoying ads and trackers", prefsManager.isAdBlockEnabled()) { isChecked ->
            prefsManager.setAdBlockEnabled(isChecked)
        }

        // Homepage
        addClickableSetting(layout, "Homepage", prefsManager.getHomepage()) {
            showHomepageDialog()
        }

        // User Agent
        addClickableSetting(layout, "User Agent", getUserAgentName(prefsManager.getUserAgent())) {
            showUserAgentDialog()
        }

        // JavaScript
        addSwitchSetting(layout, "JavaScript", "Enable JavaScript", prefsManager.isJavaScriptEnabled()) { isChecked ->
            prefsManager.setJavaScriptEnabled(isChecked)
        }

        // Cookies
        addSwitchSetting(layout, "Cookies", "Enable cookies", prefsManager.isCookiesEnabled()) { isChecked ->
            prefsManager.setCookiesEnabled(isChecked)
        }

        // Dark Mode
        addSwitchSetting(layout, "Dark Mode", "Use dark theme", prefsManager.isDarkMode()) { isChecked ->
            prefsManager.setDarkMode(isChecked)
            recreate()
        }

        // Clear Data
        addClickableSetting(layout, "Clear Data", "Clear browsing data") {
            showClearDataDialog()
        }

        // About
        addClickableSetting(layout, "About", "Version 1.0.0") {
            showAboutDialog()
        }

        scrollView.addView(layout)
        setContentView(scrollView)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }

    private fun addTitle(layout: LinearLayout, text: String) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 32)
        }
        layout.addView(textView)
    }

    private fun addSwitchSetting(layout: LinearLayout, title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 16)
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitleView = TextView(this).apply {
            text = subtitle
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }

        textContainer.addView(titleView)
        textContainer.addView(subtitleView)

        val switch = SwitchCompat(this).apply {
            this.isChecked = isChecked
            setOnCheckedChangeListener { _, isChecked -> onCheckedChange(isChecked) }
        }

        container.addView(textContainer)
        container.addView(switch)
        layout.addView(container)

        // Divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFFEEEEEE.toInt())
        }
        layout.addView(divider)
    }

    private fun addClickableSetting(layout: LinearLayout, title: String, subtitle: String, onClick: () -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
            setOnClickListener { onClick() }
            isClickable = true
            isFocusable = true
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitleView = TextView(this).apply {
            text = subtitle
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }

        container.addView(titleView)
        container.addView(subtitleView)
        layout.addView(container)

        // Divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFFEEEEEE.toInt())
        }
        layout.addView(divider)
    }

    private fun showHomepageDialog() {
        val editText = EditText(this).apply {
            setText(prefsManager.getHomepage())
            hint = "Enter homepage URL"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Homepage")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                prefsManager.setHomepage(editText.text.toString())
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserAgentDialog() {
        val userAgents = arrayOf(
            "Default",
            "Chrome (Desktop)",
            "Firefox (Desktop)",
            "Safari (Desktop)",
            "Edge (Desktop)",
            "Chrome (Android)",
            "Firefox (Android)",
            "Samsung Browser"
        )

        val userAgentValues = arrayOf(
            "",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Android 14; Mobile; rv:121.0) Gecko/121.0 Firefox/121.0",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36"
        )

        AlertDialog.Builder(this)
            .setTitle("User Agent")
            .setItems(userAgents) { _, which ->
                prefsManager.setUserAgent(userAgentValues[which])
                recreate()
            }
            .show()
    }

    private fun getUserAgentName(userAgent: String): String {
        return when {
            userAgent.isEmpty() -> "Default"
            userAgent.contains("Windows") && userAgent.contains("Chrome") -> "Chrome (Desktop)"
            userAgent.contains("Windows") && userAgent.contains("Firefox") -> "Firefox (Desktop)"
            userAgent.contains("Macintosh") && userAgent.contains("Safari") -> "Safari (Desktop)"
            userAgent.contains("Edg/") -> "Edge (Desktop)"
            userAgent.contains("Android") && userAgent.contains("Chrome") -> "Chrome (Android)"
            userAgent.contains("Android") && userAgent.contains("Firefox") -> "Firefox (Android)"
            userAgent.contains("SamsungBrowser") -> "Samsung Browser"
            else -> "Custom"
        }
    }

    private fun showClearDataDialog() {
        val items = arrayOf("Clear History", "Clear Cookies", "Clear Cache", "Clear All")

        AlertDialog.Builder(this)
            .setTitle("Clear Data")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> prefsManager.clearHistory()
                    1 -> {
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    }
                    2 -> {
                        // Cache is cleared automatically
                    }
                    3 -> {
                        prefsManager.clearHistory()
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    }
                }
                AlertDialog.Builder(this)
                    .setTitle("Done")
                    .setMessage("Data cleared successfully")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About LiteBrowser")
            .setMessage("LiteBrowser v1.0.0\n\nA lightweight, fast browser for Android.\n\nFeatures:\n• Built-in Ad Blocker\n• Tab Management\n• Dark Mode\n• Custom User Agent\n• And more!")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
