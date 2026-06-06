package com.litebrowser.data

data class TabInfo(
    val id: String,
    var title: String,
    var url: String,
    var isDesktopMode: Boolean = false
)
