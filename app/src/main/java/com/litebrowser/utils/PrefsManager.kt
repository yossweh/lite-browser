package com.litebrowser.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lite_browser_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AD_BLOCK_ENABLED = "ad_block_enabled"
        private const val KEY_HOMEPAGE = "homepage"
        private const val KEY_USER_AGENT = "user_agent"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_HISTORY = "history"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
        private const val KEY_COOKIES_ENABLED = "cookies_enabled"
    }

    // Ad Blocker
    fun isAdBlockEnabled(): Boolean {
        return prefs.getBoolean(KEY_AD_BLOCK_ENABLED, true)
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AD_BLOCK_ENABLED, enabled).apply()
    }

    // Homepage
    fun getHomepage(): String {
        return prefs.getString(KEY_HOMEPAGE, "https://www.google.com") ?: "https://www.google.com"
    }

    fun setHomepage(url: String) {
        prefs.edit().putString(KEY_HOMEPAGE, url).apply()
    }

    // User Agent
    fun getUserAgent(): String {
        return prefs.getString(KEY_USER_AGENT, "") ?: ""
    }

    fun setUserAgent(userAgent: String) {
        prefs.edit().putString(KEY_USER_AGENT, userAgent).apply()
    }

    // Bookmarks
    fun getBookmarks(): List<Pair<String, String>> {
        val bookmarksStr = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        if (bookmarksStr.isEmpty()) return emptyList()

        return bookmarksStr.split("||").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    fun addBookmark(name: String, url: String) {
        val bookmarks = getBookmarks().toMutableList()
        if (bookmarks.any { it.second == url }) return
        bookmarks.add(Pair(name, url))
        saveBookmarks(bookmarks)
    }

    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.second == url }
        saveBookmarks(bookmarks)
    }

    private fun saveBookmarks(bookmarks: List<Pair<String, String>>) {
        val bookmarksStr = bookmarks.joinToString("||") { "${it.first}|${it.second}" }
        prefs.edit().putString(KEY_BOOKMARKS, bookmarksStr).apply()
    }

    // History
    fun getHistory(): List<String> {
        val historyStr = prefs.getString(KEY_HISTORY, "") ?: ""
        if (historyStr.isEmpty()) return emptyList()
        return historyStr.split("|").filter { it.isNotEmpty() }
    }

    fun addToHistory(url: String) {
        val history = getHistory().toMutableList()
        history.remove(url)
        history.add(url)
        if (history.size > 100) {
            history.removeAt(0)
        }
        saveHistory(history)
    }

    fun clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "").apply()
    }

    private fun saveHistory(history: List<String>) {
        prefs.edit().putString(KEY_HISTORY, history.joinToString("|")).apply()
    }

    // Dark Mode
    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    // JavaScript
    fun isJavaScriptEnabled(): Boolean {
        return prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true)
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JAVASCRIPT_ENABLED, enabled).apply()
    }

    // Cookies
    fun isCookiesEnabled(): Boolean {
        return prefs.getBoolean(KEY_COOKIES_ENABLED, true)
    }

    fun setCookiesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COOKIES_ENABLED, enabled).apply()
    }
}
