package com.litebrowser.custom.popup.simple

class SimplePopupClickListener(val clickListener: (Int) -> Unit) {
    fun onClick(id: Int) = clickListener(id)
}