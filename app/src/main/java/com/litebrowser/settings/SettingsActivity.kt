package com.litebrowser.settings

import android.os.Bundle
import android.view.View
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

        // Desktop Mode toggle (simple like Via Browser)
        val switchDesktopMode = findViewById<Switch>(R.id.switchDesktopMode)
        switchDesktopMode.isChecked = prefsManager.isDesktopMode()
        switchDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setDesktopMode(isChecked)
            setResult(RESULT_OK)
        }

        // Dark Mode (3 options: Auto/On/Off like Via Browser)
        val tvDarkMode = findViewById<TextView>(R.id.tvDarkMode)
        tvDarkMode.text = when (prefsManager.getDarkMode()) {
            "on" -> "On"
            "off" -> "Off"
            else -> "Auto"
        }
        findViewById<View>(R.id.layoutDarkMode).setOnClickListener {
            showDarkModeDialog(tvDarkMode)
        }

        // Homepage setting
        val tvHomepage = findViewById<TextView>(R.id.tvHomepage)
        tvHomepage.text = prefsManager.getHomepage()
        findViewById<View>(R.id.layoutHomepage).setOnClickListener {
            showHomepageDialog(tvHomepage)
        }

        // Search Engine setting
        val tvSearchEngine = findViewById<TextView>(R.id.tvSearchEngine)
        tvSearchEngine.text = prefsManager.getSearchEngineName()
        findViewById<View>(R.id.layoutSearchEngine).setOnClickListener {
            showSearchEngineDialog(tvSearchEngine)
        }

        // Clear Cache
        findViewById<View>(R.id.layoutClearCache).setOnClickListener {
            try {
                WebView(this).clearCache(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        // Clear History
        val tvHistoryCount = findViewById<TextView>(R.id.tvHistoryCount)
        tvHistoryCount.text = "${prefsManager.getHistory().size} items"
        findViewById<View>(R.id.layoutClearHistory).setOnClickListener {
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
            tvVersion.text = "6.0.0"
        }

        // Remove Ads
        findViewById<View>(R.id.layoutRemoveAds).setOnClickListener {
            showRewardedAdDialog()
        }
    }

    private fun showDarkModeDialog(tvDarkMode: TextView) {
        val options = arrayOf("Auto (Follow System)", "On", "Off")
        val values = arrayOf("auto", "on", "off")

        AlertDialog.Builder(this)
            .setTitle("Dark Mode")
            .setItems(options) { _, which ->
                prefsManager.setDarkMode(values[which])
                tvDarkMode.text = when (values[which]) {
                    "on" -> "On"
                    "off" -> "Off"
                    else -> "Auto"
                }
                setResult(RESULT_OK)
                Toast.makeText(this, "Dark mode applied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showRewardedAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remove Ads")
            .setMessage("Watch a short video to remove ads for your current session. This helps keep the app free!")
            .setPositiveButton("Watch Video") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
