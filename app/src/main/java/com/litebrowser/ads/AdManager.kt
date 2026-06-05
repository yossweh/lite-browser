package com.litebrowser.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        
        // Test Ad Unit IDs (replace with real ones for production)
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        
        // Show interstitial every N page loads
        const val INTERSTITIAL_FREQUENCY = 5
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var pageLoadCount = 0
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true
            loadInterstitial()
            loadRewarded()
        }
    }

    fun createBannerAd(): AdView {
        return AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
        }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial loaded")
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "Interstitial failed: ${error.message}")
                interstitialAd = null
            }
        })
    }

    private fun loadRewarded() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded loaded")
                rewardedAd = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "Rewarded failed: ${error.message}")
                rewardedAd = null
            }
        })
    }

    fun onPageLoaded() {
        pageLoadCount++
        if (pageLoadCount % INTERSTITIAL_FREQUENCY == 0) {
            showInterstitial()
        }
    }

    fun showInterstitial() {
        val activity = context as? Activity ?: return
        interstitialAd?.show(activity)
        // Preload next interstitial after showing
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                loadInterstitial()
            }
        }
    }

    fun showRewarded(onRewarded: () -> Unit) {
        val activity = context as? Activity ?: return
        rewardedAd?.show(activity) { rewardItem ->
            Log.d(TAG, "Rewarded: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
            loadRewarded() // Preload next
        }
    }

    fun hasRewardedAd(): Boolean = rewardedAd != null

    fun destroy() {
        // Cleanup handled by AdMob
    }
}
