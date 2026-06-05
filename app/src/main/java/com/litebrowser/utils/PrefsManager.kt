package com.litebrowser.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("litebrowser_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AD_BLOCK = "ad_block"
        private const val KEY_HOMEPAGE = "homepage"
        private const val KEY_USER_AGENT = "user_agent"
        private const val KEY_USER_AGENT_NAME = "user_agent_name"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_SEARCH_ENGINE_NAME = "search_engine_name"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_JAVASCRIPT = "javascript"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_HISTORY = "history"
    }

    // Ad Block
    fun isAdBlockEnabled(): Boolean = prefs.getBoolean(KEY_AD_BLOCK, true)
    fun setAdBlockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AD_BLOCK, enabled).apply()

    // Homepage
    fun getHomepage(): String = prefs.getString(KEY_HOMEPAGE, "https://www.google.com") ?: "https://www.google.com"
    fun setHomepage(url: String) = prefs.edit().putString(KEY_HOMEPAGE, url).apply()

    // User Agent
    fun getUserAgent(): String = prefs.getString(KEY_USER_AGENT, 
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36") 
        ?: "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    fun setUserAgent(ua: String) = prefs.edit().putString(KEY_USER_AGENT, ua).apply()

    fun getUserAgentName(): String = prefs.getString(KEY_USER_AGENT_NAME, "Default (Mobile)") ?: "Default (Mobile)"
    fun setUserAgentName(name: String) = prefs.edit().putString(KEY_USER_AGENT_NAME, name).apply()

    // Search Engine
    fun getSearchEngine(): String = prefs.getString(KEY_SEARCH_ENGINE, "https://www.google.com/search?q=") 
        ?: "https://www.google.com/search?q="
    fun setSearchEngine(url: String, name: String) {
        prefs.edit().putString(KEY_SEARCH_ENGINE, url).apply()
        prefs.edit().putString(KEY_SEARCH_ENGINE_NAME, name).apply()
    }
    fun getSearchEngineName(): String = prefs.getString(KEY_SEARCH_ENGINE_NAME, "Google") ?: "Google"

    // Dark Mode
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    // JavaScript
    fun isJavascriptEnabled(): Boolean = prefs.getBoolean(KEY_JAVASCRIPT, true)
    fun setJavascriptEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply()

    // Bookmarks
    fun getBookmarks(): List<Pair<String, String>> {
        val bookmarksStr = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        if (bookmarksStr.isEmpty()) return emptyList()
        return bookmarksStr.split("|||").mapNotNull {
            val parts = it.split("||")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    fun addBookmark(name: String, url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.add(Pair(name, url))
        val bookmarksStr = bookmarks.joinToString("|||") { "${it.first}||${it.second}" }
        prefs.edit().putString(KEY_BOOKMARKS, bookmarksStr).apply()
    }

    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.second == url }
        val bookmarksStr = bookmarks.joinToString("|||") { "${it.first}||${it.second}" }
        prefs.edit().putString(KEY_BOOKMARKS, bookmarksStr).apply()
    }

    // History
    fun getHistory(): List<String> {
        val historyStr = prefs.getString(KEY_HISTORY, "") ?: ""
        if (historyStr.isEmpty()) return emptyList()
        return historyStr.split("|||").filter { it.isNotEmpty() }
    }

    fun addHistory(url: String) {
        val history = getHistory().toMutableList()
        history.add(url)
        // Keep only last 100 entries
        if (history.size > 100) {
            history.removeAt(0)
        }
        val historyStr = history.joinToString("|||")
        prefs.edit().putString(KEY_HISTORY, historyStr).apply()
    }

    fun clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "").apply()
    }
}
