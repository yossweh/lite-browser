package com.litebrowser.custom.popup.smart

class SmartPopupMenuItemClickListener(val clickListener: (SmartPopupMenuItem) -> Unit) {
    fun onClick(smartPopupMenuItem: SmartPopupMenuItem) = clickListener(smartPopupMenuItem)
}

