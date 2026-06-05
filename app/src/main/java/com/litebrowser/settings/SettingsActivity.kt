package com.litebrowser.settings

import android.os.Bundle
import android.webkit.WebView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.litebrowser.R
import com.litebrowser.utils.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefsManager = PrefsManager(this)

        setupSettings()
    }

    private fun setupSettings() {
        // Ad Block toggle
        val switchAdBlock = findViewById<Switch>(R.id.switchAdBlock)
        switchAdBlock.isChecked = prefsManager.isAdBlockEnabled()
        switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setAdBlockEnabled(isChecked)
        }

        // JavaScript toggle
        val switchJavascript = findViewById<Switch>(R.id.switchJavascript)
        switchJavascript.isChecked = true // Default on
        switchJavascript.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setJavascriptEnabled(isChecked)
        }

        // Dark Mode toggle
        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        switchDarkMode.isChecked = prefsManager.isDarkMode()
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setDarkMode(isChecked)
            Toast.makeText(this, "Restart app to apply dark mode", Toast.LENGTH_SHORT).show()
        }

        // Homepage setting
        val tvHomepage = findViewById<TextView>(R.id.tvHomepage)
        tvHomepage.text = prefsManager.getHomepage()
        findViewById<android.view.View>(R.id.layoutHomepage).setOnClickListener {
            showHomepageDialog(tvHomepage)
        }

        // Search Engine setting
        val tvSearchEngine = findViewById<TextView>(R.id.tvSearchEngine)
        tvSearchEngine.text = prefsManager.getSearchEngineName()
        findViewById<android.view.View>(R.id.layoutSearchEngine).setOnClickListener {
            showSearchEngineDialog(tvSearchEngine)
        }

        // User Agent
        val tvUserAgent = findViewById<TextView>(R.id.tvUserAgent)
        tvUserAgent.text = prefsManager.getUserAgentName()
        findViewById<android.view.View>(R.id.layoutUserAgent).setOnClickListener {
            showUserAgentDialog(tvUserAgent)
        }

        // Clear Cache
        findViewById<android.view.View>(R.id.layoutClearCache).setOnClickListener {
            WebView(this).clearCache(true)
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        // Clear History
        val tvHistoryCount = findViewById<TextView>(R.id.tvHistoryCount)
        tvHistoryCount.text = "${prefsManager.getHistory().size} items"
        findViewById<android.view.View>(R.id.layoutClearHistory).setOnClickListener {
            prefsManager.clearHistory()
            tvHistoryCount.text = "0 items"
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
        }

        // Version info
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = pInfo.versionName
        } catch (e: Exception) {
            tvVersion.text = "3.0.0"
        }

        // Remove Ads
        findViewById<android.view.View>(R.id.layoutRemoveAds).setOnClickListener {
            showRewardedAdDialog()
        }
    }

    private fun showHomepageDialog(tvHomepage: TextView) {
        val editText = android.widget.EditText(this)
        editText.setText(prefsManager.getHomepage())
        editText.hint = "https://www.google.com"

        AlertDialog.Builder(this)
            .setTitle("Set Homepage")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefsManager.setHomepage(url)
                    tvHomepage.text = url
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchEngineDialog(tvSearchEngine: TextView) {
        val engines = arrayOf("Google", "Bing", "DuckDuckGo", "Yahoo", "Yandex")
        val urls = arrayOf(
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://search.yahoo.com/search?p=",
            "https://yandex.com/search/?text="
        )

        AlertDialog.Builder(this)
            .setTitle("Search Engine")
            .setItems(engines) { _, which ->
                prefsManager.setSearchEngine(urls[which], engines[which])
                tvSearchEngine.text = engines[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserAgentDialog(tvUserAgent: TextView) {
        val agents = arrayOf(
            "Default (Mobile)",
            "Chrome Desktop",
            "Firefox Desktop",
            "Safari iPhone",
            "Custom"
        )

        AlertDialog.Builder(this)
            .setTitle("User Agent")
            .setItems(agents) { _, which ->
                val ua = when (which) {
                    0 -> "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    1 -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    2 -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
                    3 -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                    4 -> {
                        showCustomUserAgentDialog(tvUserAgent)
                        return@setItems
                    }
                    else -> return@setItems
                }
                prefsManager.setUserAgent(ua)
                prefsManager.setUserAgentName(agents[which])
                tvUserAgent.text = agents[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomUserAgentDialog(tvUserAgent: TextView) {
        val editText = android.widget.EditText(this)
        editText.setText(prefsManager.getUserAgent())
        editText.hint = "Enter custom user agent"

        AlertDialog.Builder(this)
            .setTitle("Custom User Agent")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val ua = editText.text.toString().trim()
                if (ua.isNotEmpty()) {
                    prefsManager.setUserAgent(ua)
                    prefsManager.setUserAgentName("Custom")
                    tvUserAgent.text = "Custom"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRewardedAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remove Ads")
            .setMessage("Watch a short video to remove ads for your current session. This helps keep the app free!")
            .setPositiveButton("Watch Video") { _, _ ->
                // This will be handled by MainActivity
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
