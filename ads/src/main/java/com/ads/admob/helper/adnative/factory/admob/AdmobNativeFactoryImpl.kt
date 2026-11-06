package com.ads.admob.helper.adnative.factory.admob

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.ads.admob.AdmobManager
import com.ads.admob.R
import com.ads.admob.data.ContentAd
import com.ads.admob.getAdRequest
import com.ads.admob.listener.NativeAdCallback
import com.google.android.material.card.MaterialCardView


class AdmobNativeFactoryImpl : AdmobNativeFactory {
    override fun requestNativeAd(context: Context, adId: String, adCallback: NativeAdCallback) {
        val builder = AdLoader.Builder(context, adId)

        val videoOptions =
            VideoOptions.Builder().setStartMuted(true).build()

        val adOptions = com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
            .setVideoOptions(videoOptions).build()

        builder.withNativeAdOptions(adOptions)
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { nativeAd ->
                adCallback.onAdLoaded(ContentAd.AdmobAd.ApNativeAd(nativeAd))
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adCallback.onAdFailedToLoad(error)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdmobManager.adsClicked()
                    adCallback.onAdClicked()
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(getAdRequest())
    }

    override fun populateNativeAdView(
        activity: Context,
        nativeAd: NativeAd,
        nativeAdViewId: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        adCallback: NativeAdCallback
    ) {
        val adView = LayoutInflater.from(activity).inflate(nativeAdViewId, null) as NativeAdView
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        val btnAction =  adView.findViewById<AppCompatButton>(R.id.ad_call_to_action)
        adView.callToActionView = btnAction

        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        val btnDown = adView.findViewById<AppCompatImageView>(R.id.btnDown)

        btnDown?.setOnClickListener {
            adView.mediaView?.visibility = View.GONE
            btnDown.visibility = View.GONE
            adView.callToActionView?.let { view ->
                (view as TextView).text =  nativeAd.callToAction
            }
            adCallback.populateNativeAd()
        }

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { (adView.mediaView)?.mediaContent = it }


        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        nativeAd.body?.let {
            adView.bodyView?.visibility = View.VISIBLE
            adView.bodyView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.bodyView?.visibility = View.INVISIBLE
        }

        nativeAd.callToAction?.let {
            adView.callToActionView?.visibility = View.VISIBLE
            adView.callToActionView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.callToActionView?.visibility = View.INVISIBLE
        }

        nativeAd.icon?.let {
            adView.iconView?.visibility = View.VISIBLE
            adView.iconView?.let { view ->
                (view as ImageView).setImageDrawable(it.drawable)
            }
        } ?: kotlin.run {
            adView.iconView?.visibility = View.GONE
        }
        nativeAd.price?.let {
            adView.priceView?.visibility = View.VISIBLE
            adView.priceView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.priceView?.visibility = View.INVISIBLE
        }

        nativeAd.starRating?.let {
            adView.starRatingView?.visibility = View.VISIBLE
            adView.starRatingView?.let { view ->
                (view as RatingBar).rating = it.toFloat()
            }
        } ?: kotlin.run {
            adView.starRatingView?.visibility = View.INVISIBLE
        }
        nativeAd.advertiser?.let {
            adView.advertiserView?.visibility = View.VISIBLE
            adView.advertiserView?.let { view ->
                (view as TextView).text = it
            }

        } ?: kotlin.run {
            adView.advertiserView?.visibility = View.INVISIBLE
        }
        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null && mediaContent.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        super.onVideoEnd()
                    }
                }
        }
        try {
//            adPlaceHolder.visibility = View.VISIBLE
            adPlaceHolder.removeAllViews()
            adPlaceHolder.addView(adView)
            containerShimmerLoading?.visibility = View.GONE
        } catch (ex: Exception) {
            adCallback.onAdFailedToShow(AdError(99, ex.message.toString(), ""))
        }
    }
}