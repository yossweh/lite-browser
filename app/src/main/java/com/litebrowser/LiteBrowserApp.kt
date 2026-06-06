package com.litebrowser

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.litebrowser.helper.PrefManager

class LiteBrowserApp : Application() {

    companion object {
        lateinit var instance: LiteBrowserApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize preferences
        PrefManager.init(this)

        // Initialize AdMob SDK
        MobileAds.initialize(this) {}
    }
}
