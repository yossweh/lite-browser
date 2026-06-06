package com.litebrowser.helper

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object AdBlocker {

    private val blockedDomains: MutableSet<String> = mutableSetOf(
        // Google Ads
        "googleadservices.com",
        "pagead2.googlesyndication.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "adservice.google.co.uk",
        "googleads.g.doubleclick.net",
        "tpc.googlesyndication.com",
        "www.googleadservices.com",
        "securepubads.g.doubleclick.net",
        "ad.doubleclick.net",
        "static.doubleclick.net",
        "m.doubleclick.net",
        "doubleclick.net",
        "googlesyndication.com",
        "s0.2mdn.net",
        "s1.2mdn.net",
        "gcdn.2mdn.net",
        "ads.google.com",
        "analytics.google.com",
        // Facebook / Meta
        "pixel.facebook.com",
        "connect.facebook.net",
        "graph.facebook.com",
        "an.facebook.com",
        "www.facebook.com/tr",
        "ads.facebook.com",
        "analytics.facebook.com",
        "staticxx.facebook.com",
        // Ad networks
        "ads.yahoo.com",
        "adserver.yahoo.com",
        "advertising.com",
        "adtech.de",
        "adtechus.com",
        "adnxs.com",
        "ads.adnxs.com",
        "akamaized.net",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "ams1-ib.adnxs.com",
        "ib.adnxs.com",
        "media.net",
        "adservetx.media.net",
        "contextual.media.net",
        "c.amazon-adsystem.com",
        // Taboola / Outbrain
        "taboola.com",
        "cdn.taboola.com",
        "trc.taboola.com",
        "api.taboola.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "log.outbrain.com",
        // AdMob
        "admob.com",
        "mediation.admob.com",
        "apps.admob.com",
        // Analytics & Tracking
        "google-analytics.com",
        "ssl.google-analytics.com",
        "www.google-analytics.com",
        "stats.g.doubleclick.net",
        "hotjar.com",
        "script.hotjar.com",
        "vars.hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "heapanalytics.com",
        "clarity.ms",
        "bat.bing.com",
        "c.bing.com",
        // Adware / Malware domains
        "adware.com",
        "adsrvr.org",
        "adform.net",
        "adcolony.com",
        "adsafeprotected.com",
        "ad.sxp.smartclip.net",
        "app-measurement.com",
        "appier.net",
        "bat.bing.com",
        "brealtime.com",
        "bidswitch.net",
        "bluekai.com",
        "casalemedia.com",
        "cdn.krxd.net",
        "cdn.segment.com",
        "chartbeat.com",
        "chartbeat.net",
        "circularhub.com",
        "cognitivlabs.com",
        "conversionruler.com",
        "crwdcntrl.net",
        "demdex.net",
        "domdex.com",
        "dotomi.com",
        "doubleverify.com",
        "e-m.fr",
        "exelator.com",
        "eyeota.net",
        "facebook.com/fr",
        "flashtalking.com",
        "fwmrm.net",
        "gemius.pl",
        "gumgum.com",
        "iasds01.com",
        "indexww.com",
        "insurads.com",
        "intentmedia.net",
        "intentmedia.net",
        "ipredictive.com",
        "krxd.net",
        "lijit.com",
        "liveintent.com",
        "localytics.com",
        "lotame.com",
        "mathtag.com",
        "maxcdn.com",
        "mediavine.com",
        "moatads.com",
        "mookie1.com",
        "mopub.com",
        "morgdog.springserve.com",
        "narrativ.com",
        "nativo.com",
        "netmng.com",
        "newrelic.com",
        "nr-data.net",
        "omaze.com",
        "omtrdc.net",
        "onaudience.com",
        "openx.net",
        "openx.com",
        "openxcdn.net",
        "optimizely.com",
        "outbrainimg.com",
        "pardot.com",
        "permutive.com",
        "permutive.app",
        "pi.pardot.com",
        "pippio.com",
        "playground.xyz",
        "popads.net",
        "popcash.net",
        "popin.cc",
        "pro-market.net",
        "pubmatic.com",
        "puds.ucweb.com",
        "punosy.com",
        "pushame.com",
        "pushance.com",
        "pushno.com",
        "pushwoosh.com",
        "quantserve.com",
        "quantcast.com",
        "quantcount.com",
        "redisquant.com",
        "remarketstats.com",
        "responsys.net",
        "revjet.com",
        "rfihub.com",
        "richmediaads.com",
        "rlcdn.com",
        "rum-http-intake.logs.datadoghq.com",
        "s.adroll.com",
        "sail-horizon.com",
        "sc-static.net",
        "scorecardresearch.com",
        "sekindo.com",
        "servebom.com",
        "sharethrough.com",
        "simpli.fi",
        "sitescout.com",
        "smaato.net",
        "smartadserver.com",
        "smartclip.net",
        "spotxchange.com",
        "spotx.tv",
        "springserve.com",
        "stickyadstv.com",
        "sumo.com",
        "sumome.com",
        "supersonicads.com",
        "surveygizmobeacon.s3.amazonaws.com",
        "switchadhub.com",
        "taboola.com",
        "tagger.opecloud.com",
        "tapad.com",
        "tapjoy.com",
        "tapjoyads.com",
        "tds.tele2.net",
        "tidaltv.com",
        "trafficjunky.com",
        "trafficjunky.net",
        "tribalfusion.com",
        "turn.com",
        "undertone.com",
        "unrulymedia.com",
        "us-u.openx.net",
        "veeseo.com",
        "vidible.tv",
        "vidoomy.com",
        "visx.net",
        "w55c.net",
        "weather.com",
        "widespace.com",
        "yieldmo.com",
        "yieldoptimizer.com",
        "yldmgrimg.net",
        "zemanta.com",
        "zergnet.com",
        "ziffdavis.com",
        "zedo.com",
        "zwaar.org"
    )

    fun shouldBlock(url: String): Boolean {
        if (url.isBlank()) return false
        val host = try {
            Uri.parse(url).host?.lowercase() ?: return false
        } catch (e: Exception) {
            return false
        }
        return blockedDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
    }

    fun loadHosts(context: Context) {
        try {
            val inputStream = context.assets.open("hosts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val host = parts[1].lowercase()
                            if (host != "localhost" && host != "0.0.0.0" && host != "127.0.0.1") {
                                blockedDomains.add(host)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // hosts.txt not available, using built-in blocklist
            e.printStackTrace()
        }
    }
}
