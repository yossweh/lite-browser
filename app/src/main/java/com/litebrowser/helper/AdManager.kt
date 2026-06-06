package com.litebrowser.helper

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    // Test ad unit IDs (replace with real ones for production)
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Interstitial frequency control
    private const val INTERSTITIAL_PAGE_LOAD_INTERVAL = 15
    private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes

    private var pageLoadCount = 0
    private var lastInterstitialShownTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
        isInitialized = true
    }

    fun createBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun shouldShowInterstitial(): Boolean {
        pageLoadCount++
        if (pageLoadCount < INTERSTITIAL_PAGE_LOAD_INTERVAL) return false

        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownTime < INTERSTITIAL_COOLDOWN_MS) return false

        pageLoadCount = 0
        return true
    }

    fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial ad not ready")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                lastInterstitialShownTime = System.currentTimeMillis()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                interstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
            }
        }

        ad.show(activity)
    }

    fun loadRewarded(context: Context) {
        if (rewardedAd != null) return

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
            }
        }

        ad.show(activity) {
            Log.d(TAG, "User rewarded")
            onRewarded()
        }
    }
}
