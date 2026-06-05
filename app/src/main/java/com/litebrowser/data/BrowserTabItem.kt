package com.litebrowser.data

import android.graphics.Bitmap
import com.litebrowser.custom.view.BrowserView

data class BrowserTabItem(
    var fullSnap: Bitmap?,
    var title: String,
    var url: String,
    var tab: BrowserView
)