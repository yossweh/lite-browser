package com.litebrowser.browser

import android.content.Context
import android.content.SharedPreferences

class TabManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tabs", Context.MODE_PRIVATE)
    private val tabs = mutableListOf<String>()

    init {
        loadTabs()
    }

    fun addTab(url: String) {
        if (!tabs.contains(url)) {
            tabs.add(url)
            saveTabs()
        }
    }

    fun removeTab(url: String) {
        tabs.remove(url)
        saveTabs()
    }

    fun getTabs(): List<String> {
        return tabs.toList()
    }

    fun getTabCount(): Int {
        return tabs.size
    }

    private fun saveTabs() {
        prefs.edit().putString("tabs", tabs.joinToString("|")).apply()
    }

    private fun loadTabs() {
        val saved = prefs.getString("tabs", "")
        if (!saved.isNullOrEmpty()) {
            tabs.clear()
            tabs.addAll(saved.split("|"))
        }
    }
}
