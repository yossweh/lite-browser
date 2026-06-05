package com.litebrowser.helper.client

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.view.View.VISIBLE
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.edsuns.adfilter.AdFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.litebrowser.R
import com.litebrowser.activity.BrowserActivity
import com.litebrowser.activity.MainActivity
import com.litebrowser.application.ESearchApplication.Companion.database
import com.litebrowser.custom.view.BrowserView
import com.litebrowser.data.BrowserTabs.updateBottomNav
import com.litebrowser.data.DataArrays.headers
import com.litebrowser.data.SharedPreferencesAccess.AD_BLOCK
import com.litebrowser.data.SharedPreferencesAccess.COOKIES
import com.litebrowser.data.SharedPreferencesAccess.DOM_STORAGE
import com.litebrowser.data.SharedPreferencesAccess.EYE_PROTECTION
import com.litebrowser.data.SharedPreferencesAccess.GET
import com.litebrowser.data.SharedPreferencesAccess.IMAGE_LOADING
import com.litebrowser.data.SharedPreferencesAccess.JS
import com.litebrowser.data.SharedPreferencesAccess.LOCATION_ACCESS
import com.litebrowser.data.SharedPreferencesAccess.POPUPS
import com.litebrowser.data.SharedPreferencesAccess.SAVE_HISTORY
import com.litebrowser.data.SharedPreferencesAccess.getSetting
import com.litebrowser.data.SharedPreferencesAccess.needToChangeBrowserSettings
import com.litebrowser.database.hist.History
import com.litebrowser.extensions.Extensions.fetchFavicon
import com.litebrowser.extensions.Extensions.forceNightMode
import com.litebrowser.extensions.Extensions.isDesktop
import com.litebrowser.extensions.Extensions.toByteArray
import com.litebrowser.functions.Functions.delayedDoInBackground
import com.litebrowser.functions.ScriptsJS.desktopScript
import com.litebrowser.functions.ScriptsJS.disBugsnag
import com.litebrowser.functions.ScriptsJS.disSentry
import com.litebrowser.functions.ScriptsJS.doNotTrackScript1
import com.litebrowser.functions.ScriptsJS.doNotTrackScript2
import com.litebrowser.functions.ScriptsJS.doNotTrackScript3
import com.litebrowser.functions.ScriptsJS.privacyScript
import com.litebrowser.helper.adblock.AdBlocker.getDomain
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

class WebClient(
    private val context: Context,
    private val progressBar: LinearProgressIndicator?
) : WebViewClient() {

    private val filter = AdFilter.get()

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {

        if ((view as? BrowserView)?.goBack == true) return false

        val url = request.url.toString()

        if (context is MainActivity) {
            for (i in context.supportFragmentManager.fragments) {
                if (i.tag == "results") {
                    val intent = Intent(context, BrowserActivity::class.java)
                    intent.putExtra("url", url)
                    context.startActivity(intent)
                    break
                }
            }
        } else {
            if (URLUtil.isNetworkUrl(url)) view.loadUrl(url, headers)
            else if (url.startsWith("intent://")) {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val extraUrl = intent.getStringExtra("browser_fallback_url")
                extraUrl?.let { view.loadUrl(it, headers) }
            } else {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (error: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.appError, Toast.LENGTH_LONG).show()
                }
            }
        }

        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        progressBar?.visibility = VISIBLE

        filter.performScript(view, url)

        if (context is BrowserActivity) {
            view?.url?.let { nonNullUrl ->
                context.searchView?.setText(nonNullUrl.getDomain())
                context.lastUrl = nonNullUrl
                context.clickedGo = false
                context.iconView?.visibility = VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    val icon = getIcon(nonNullUrl)
                    context.iconView?.let {
                        Glide.with(context.applicationContext).load(icon).into(it)
                    }
                }
            }
        }
        super.onPageStarted(view, url, favicon)
    }


    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {

        val ctx = context
        if (ctx is BrowserActivity) ctx.updateBottomNav()

        if (needToChangeBrowserSettings(context, GET)) {
            val manager = CookieManager.getInstance()
            if (getSetting(context, COOKIES)) {
                manager.setAcceptCookie(true)
                manager.getCookie(url)
            } else manager.setAcceptCookie(false)

            view.settings.apply {
                javaScriptEnabled = getSetting(context, JS)
                domStorageEnabled = getSetting(context, DOM_STORAGE)
                blockNetworkImage = !getSetting(context, IMAGE_LOADING)
                javaScriptCanOpenWindowsAutomatically = getSetting(context, POPUPS)
                setGeolocationEnabled(getSetting(context, LOCATION_ACCESS))
            }

            if (getSetting(context, EYE_PROTECTION)) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
                    WebSettingsCompat.setForceDark(view.settings, WebSettingsCompat.FORCE_DARK_ON)
                else view.forceNightMode(true)
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
                    WebSettingsCompat.setForceDark(
                        view.settings,
                        WebSettingsCompat.FORCE_DARK_OFF
                    )
                else view.forceNightMode(false)
            }
        }

        if (!isReload && getSetting(context, SAVE_HISTORY)) {

            view.url?.let {
                if (context is BrowserActivity) {
                    context.searchView?.setText(it.getDomain())
                    context.lastUrl = it
                    context.clickedGo = false

                    context.updateBottomNav()
                }

                val calendar = Calendar.getInstance()
                val strDay = calendar[Calendar.DAY_OF_MONTH]
                val strMonth = calendar[Calendar.MONTH]
                val year = calendar[Calendar.YEAR]
                val strHour = calendar[Calendar.HOUR_OF_DAY]
                val strMinute = calendar[Calendar.MINUTE]
                val minute = when {
                    strMinute < 10 -> "0$strMinute"
                    else -> "$strMinute"
                }
                val hour = when {
                    strHour < 10 -> "0$strHour"
                    else -> "$strHour"
                }

                val day = when {
                    strDay < 10 -> "0$strDay"
                    else -> "$strDay"
                }
                val month = when {
                    strMonth + 1 < 10 -> "0${strMonth + 1}"
                    else -> "${strMonth + 1}"
                }

                val stringMonth = DateFormatSymbols(Locale.getDefault()).months[strMonth]

                val sortingString = "$day-$month-$year | $hour:$minute"

                var title = ""
                Handler(context.mainLooper).postDelayed({
                    title = view.title!!
                    if (title.isEmpty()) title = it
                }, 400)

                delayedDoInBackground(500) {
                    val icon = context.fetchFavicon(it).toByteArray()
                    val dao = database.historyDao()

                    val item = History(
                        title,
                        it,
                        icon,
                        "${hour}:${minute}",
                        "$strDay $stringMonth $year",
                        sortingString
                    )

                    if (context is MainActivity) {
                        for (i in context.supportFragmentManager.fragments) {
                            if (i.tag == "results") {
                                dao.insert(item)
                                break
                            }
                        }
                    } else {
                        dao.insert(item)
                    }
                }
            }
        }

        super.doUpdateVisitedHistory(view, url, isReload)
    }

    private suspend fun getIcon(url: String): Bitmap = withContext(Dispatchers.IO) {
        return@withContext context.fetchFavicon(url)
    }

    override fun onLoadResource(view: WebView, url: String?) {
        if (view.isDesktop()) view.evaluateJavascript(desktopScript, null)
        view.apply {
            evaluateJavascript(privacyScript, null)
            evaluateJavascript(doNotTrackScript1, null)
            evaluateJavascript(doNotTrackScript2, null)
            evaluateJavascript(doNotTrackScript3, null)
            evaluateJavascript(disSentry, null)
            evaluateJavascript(disBugsnag, null)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        (view as? BrowserView)?.goBack = false
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (!getSetting(context, AD_BLOCK)) return super.shouldInterceptRequest(view, request)

        val result = filter.shouldIntercept(view!!, request)
        return result.resourceResponse
    }
}