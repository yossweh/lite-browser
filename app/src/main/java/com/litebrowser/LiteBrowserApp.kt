package com.litebrowser

import android.app.Application
import com.google.android.gms.ads.MobileAds

class LiteBrowserApp : Application() {

    companion object {
        lateinit var instance: LiteBrowserApp
            private set

        /** Set of ad-serving hostnames to block. */
        val adHosts: MutableSet<String> = mutableSetOf()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize AdMob SDK
        MobileAds.initialize(this) { initializationStatus ->
            // AdMob is ready – adapters can be checked via initializationStatus
        }

        // Initialize built-in ad-blocker hosts list
        initAdBlockerHosts()
    }

    /**
     * Populate [adHosts] with a default set of known advertising domains.
     * In a production build this could be loaded from a bundled hosts file
     * or updated from a remote source.
     */
    private fun initAdBlockerHosts() {
        adHosts.clear()
        adHosts.addAll(
            listOf(
                "pagead2.googlesyndication.com",
                "adservice.google.com",
                "www.googleadservices.com",
                "googleads.g.doubleclick.net",
                "stats.g.doubleclick.net",
                "ad.doubleclick.net",
                "static.doubleclick.net",
                "m.doubleclick.net",
                "mediavisor.doubleclick.net",
                "ads.yahoo.com",
                "adserver.yahoo.com",
                "analytics.yahoo.com",
                "ad.facebook.com",
                "ads.facebook.com",
                "www.facebook.com",
                "pixel.facebook.com",
                "ads-twitter.com",
                "ads-api.twitter.com",
                "ads.linkedin.com",
                "ad.atdmt.com",
                "ads.msn.com",
                "adnxs.com",
                "ib.adnxs.com",
                "ads.pubmatic.com",
                "ad.turn.com",
                "advertising.com",
                "adcolony.com",
                "ads.tapjoy.com",
                "tracking.i2w.io",
                "securepubads.g.doubleclick.net",
                "tpc.googlesyndication.com",
                "www.googletagservices.com",
                "adservice.google.co.uk",
                "pagead2.googlesyndication.com",
                "tpc.googlesyndication.com",
                "s0.2mdn.net",
                "s1.2mdn.net"
            )
        )
    }
}
