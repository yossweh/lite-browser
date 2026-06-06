package com.litebrowser.helper

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("litebrowser_prefs", Context.MODE_PRIVATE)

    var homepage: String
        get() = prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE) ?: DEFAULT_HOMEPAGE
        set(value) = prefs.edit().putString(KEY_HOMEPAGE, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, DEFAULT_SEARCH_ENGINE) ?: DEFAULT_SEARCH_ENGINE
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK_ENABLED, value).apply()

    var desktopModeDefault: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE_DEFAULT, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE_DEFAULT, value).apply()

    companion object {
        private const val KEY_HOMEPAGE = "homepage"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AD_BLOCK_ENABLED = "ad_block_enabled"
        private const val KEY_DESKTOP_MODE_DEFAULT = "desktop_mode_default"

        const val DEFAULT_HOMEPAGE = "https://www.google.com"
        const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="
    }
}
