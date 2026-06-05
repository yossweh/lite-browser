package com.litebrowser.adblock

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class AdBlocker(context: Context) {

    private val adDomains = mutableSetOf<String>()
    private val adPatterns = mutableListOf<Regex>()

    init {
        loadAdBlockLists(context)
    }

    private fun loadAdBlockLists(context: Context) {
        try {
            // Load built-in ad domains
            val inputStream = context.assets.open("adblock_hosts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            adDomains.add(parts[1].lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to basic ad blocking
            loadBasicAdDomains()
        }

        // Load common ad patterns
        loadAdPatterns()
    }

    private fun loadBasicAdDomains() {
        val basicAdDomains = listOf(
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "google-analytics.com",
            "googletagmanager.com",
            "facebook.com/tr",
            "ads-twitter.com",
            "ads.yahoo.com",
            "adnxs.com",
            "adsrvr.org",
            "adtechus.com",
            "advertising.com",
            "amazon-adsystem.com",
            "appnexus.com",
            "bidswitch.net",
            "casalemedia.com",
            "chartbeat.com",
            "criteo.com",
            "demdex.net",
            "doubleverify.com",
            "exelator.com",
            "eyeota.net",
            "flashtalking.com",
            "fwmrm.net",
            "google.com/ads",
            "google.com/pagead",
            "gstatic.com/generate_204",
            "hotjar.com",
            "imasdk.googleapis.com",
            "insurads.com",
            "krxd.net",
            "lijit.com",
            "liveintent.com",
            "lotame.com",
            "mathtag.com",
            "media.net",
            "moatads.com",
            "mookie1.com",
            "narrative.io",
            "nativo.com",
            "netmng.com",
            "newrelic.com",
            "nexusmedia.com",
            "ninthdecimal.com",
            "nrcdn.com",
            "omaze.com",
            "openx.net",
            "optimizely.com",
            "outbrain.com",
            "pardot.com",
            "permutive.com",
            "pippio.com",
            "plista.com",
            "popads.net",
            "popcash.net",
            "pubmatic.com",
            "quantserve.com",
            "rayjump.com",
            "revjet.com",
            "rfihub.com",
            "rlcdn.com",
            "rubiconproject.com",
            "sail-horizon.com",
            "scorecardresearch.com",
            "segment.com",
            "serving-sys.com",
            "sharethrough.com",
            "simpli.fi",
            "sitescout.com",
            "smartadserver.com",
            "spotxchange.com",
            "stickyadstv.com",
            "taboola.com",
            "tapad.com",
            "tidaltv.com",
            "trafficjunky.com",
            "tribalfusion.com",
            "turn.com",
            "twimg.com/ads",
            "tynt.com",
            "undertone.com",
            "urbaninsight.com",
            "veeseo.com",
            "vidible.tv",
            "visiblemeasures.com",
            "voicefive.com",
            "w55c.net",
            "wdads.com",
            "whiteops.com",
            "widespace.com",
            "wishabi.com",
            "wtg-ads.com",
            "yieldmo.com",
            "yieldoptimizer.com",
            "zdbb.net",
            "zergnet.com",
            "zwaar.org"
        )
        adDomains.addAll(basicAdDomains)
    }

    private fun loadAdPatterns() {
        val patterns = listOf(
            ".*\\.doubleclick\\.net.*",
            ".*\\.googleadservices\\.com.*",
            ".*\\.googlesyndication\\.com.*",
            ".*\\.google-analytics\\.com.*",
            ".*\\.googletagmanager\\.com.*",
            ".*\\.facebook\\.com/tr.*",
            ".*\\.ads-twitter\\.com.*",
            ".*\\.ads\\.yahoo\\.com.*",
            ".*\\.adnxs\\.com.*",
            ".*\\.adsrvr\\.org.*",
            ".*\\.adtechus\\.com.*",
            ".*\\.advertising\\.com.*",
            ".*\\.amazon-adsystem\\.com.*",
            ".*\\.appnexus\\.com.*",
            ".*\\.bidswitch\\.net.*",
            ".*\\.casalemedia\\.com.*",
            ".*\\.chartbeat\\.com.*",
            ".*\\.criteo\\.com.*",
            ".*\\.demdex\\.net.*",
            ".*\\.doubleverify\\.com.*",
            ".*\\.exelator\\.com.*",
            ".*\\.eyeota\\.net.*",
            ".*\\.flashtalking\\.com.*",
            ".*\\.fwmrm\\.net.*",
            ".*\\.google\\.com/ads.*",
            ".*\\.google\\.com/pagead.*",
            ".*\\.gstatic\\.com/generate_204.*",
            ".*\\.hotjar\\.com.*",
            ".*\\.imasdk\\.googleapis\\.com.*",
            ".*\\.insurads\\.com.*",
            ".*\\.krxd\\.net.*",
            ".*\\.lijit\\.com.*",
            ".*\\.liveintent\\.com.*",
            ".*\\.lotame\\.com.*",
            ".*\\.mathtag\\.com.*",
            ".*\\.media\\.net.*",
            ".*\\.moatads\\.com.*",
            ".*\\.mookie1\\.com.*",
            ".*\\.narrative\\.io.*",
            ".*\\.nativo\\.com.*",
            ".*\\.netmng\\.com.*",
            ".*\\.newrelic\\.com.*",
            ".*\\.nexusmedia\\.com.*",
            ".*\\.ninthdecimal\\.com.*",
            ".*\\.nrcdn\\.com.*",
            ".*\\.omaze\\.com.*",
            ".*\\.openx\\.net.*",
            ".*\\.optimizely\\.com.*",
            ".*\\.outbrain\\.com.*",
            ".*\\.pardot\\.com.*",
            ".*\\.permutive\\.com.*",
            ".*\\.pippio\\.com.*",
            ".*\\.plista\\.com.*",
            ".*\\.popads\\.net.*",
            ".*\\.popcash\\.net.*",
            ".*\\.pubmatic\\.com.*",
            ".*\\.quantserve\\.com.*",
            ".*\\.rayjump\\.com.*",
            ".*\\.revjet\\.com.*",
            ".*\\.rfihub\\.com.*",
            ".*\\.rlcdn\\.com.*",
            ".*\\.rubiconproject\\.com.*",
            ".*\\.sail-horizon\\.com.*",
            ".*\\.scorecardresearch\\.com.*",
            ".*\\.segment\\.com.*",
            ".*\\.serving-sys\\.com.*",
            ".*\\.sharethrough\\.com.*",
            ".*\\.simpli\\.fi.*",
            ".*\\.sitescout\\.com.*",
            ".*\\.smartadserver\\.com.*",
            ".*\\.spotxchange\\.com.*",
            ".*\\.stickyadstv\\.com.*",
            ".*\\.taboola\\.com.*",
            ".*\\.tapad\\.com.*",
            ".*\\.tidaltv\\.com.*",
            ".*\\.trafficjunky\\.com.*",
            ".*\\.tribalfusion\\.com.*",
            ".*\\.turn\\.com.*",
            ".*\\.twimg\\.com/ads.*",
            ".*\\.tynt\\.com.*",
            ".*\\.undertone\\.com.*",
            ".*\\.urbaninsight\\.com.*",
            ".*\\.veeseo\\.com.*",
            ".*\\.vidible\\.tv.*",
            ".*\\.visiblemeasures\\.com.*",
            ".*\\.voicefive\\.com.*",
            ".*\\.w55c\\.net.*",
            ".*\\.wdads\\.com.*",
            ".*\\.whiteops\\.com.*",
            ".*\\.widespace\\.com.*",
            ".*\\.wishabi\\.com.*",
            ".*\\.wtg-ads\\.com.*",
            ".*\\.yieldmo\\.com.*",
            ".*\\.yieldoptimizer\\.com.*",
            ".*\\.zdbb\\.net.*",
            ".*\\.zergnet\\.com.*",
            ".*\\.zwaar\\.org.*"
        )

        patterns.forEach { pattern ->
            try {
                adPatterns.add(Regex(pattern))
            } catch (e: Exception) {
                // Skip invalid patterns
            }
        }
    }

    fun isAd(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Check domain list
        for (domain in adDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        // Check patterns
        for (pattern in adPatterns) {
            if (pattern.matches(lowerUrl)) {
                return true
            }
        }

        return false
    }
}
