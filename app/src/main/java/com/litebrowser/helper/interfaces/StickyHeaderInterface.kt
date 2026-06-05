package com.litebrowser.helper.interfaces

import android.view.View

interface StickyHeaderInterface {
    fun getHeaderPositionForItem(itemPosition: Int): Int
    fun bindHeaderData(header: View?, headerPosition: Int)
    fun isHeader(itemPosition: Int): Boolean
}