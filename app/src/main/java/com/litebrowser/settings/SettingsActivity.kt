package com.litebrowser.settings

import android.os.Bundle
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
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

        // Homepage setting
        val tvHomepage = findViewById<TextView>(R.id.tvHomepage)
        tvHomepage.text = "Homepage: ${prefsManager.getHomepage()}"
        tvHomepage.setOnClickListener {
            showHomepageDialog(tvHomepage)
        }

        // User Agent
        val spinnerUA = findViewById<Spinner>(R.id.spinnerUserAgent)
        val uaOptions = arrayOf(
            "Default (Mobile)",
            "Chrome Desktop",
            "Firefox Desktop",
            "Safari iPhone"
        )
        spinnerUA.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uaOptions)
        
        val currentUA = prefsManager.getUserAgent()
        spinnerUA.setSelection(when {
            currentUA.contains("Windows") -> 1
            currentUA.contains("Firefox") -> 2
            currentUA.contains("iPhone") -> 3
            else -> 0
        })
        
        spinnerUA.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val ua = when (position) {
                    1 -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    2 -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
                    3 -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                    else -> "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                prefsManager.setUserAgent(ua)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Clear data
        findViewById<android.widget.Button>(R.id.btnClearData).setOnClickListener {
            WebView(this).clearCache(true)
            prefsManager.clearHistory()
            android.widget.Toast.makeText(this, "Data cleared", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Version info
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version: ${pInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "Version: 3.0.0"
        }
    }

    private fun showHomepageDialog(tvHomepage: TextView) {
        val editText = android.widget.EditText(this)
        editText.setText(prefsManager.getHomepage())
        editText.hint = "https://www.google.com"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set Homepage")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefsManager.setHomepage(url)
                    tvHomepage.text = "Homepage: $url"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
