package com.neilturner.aerialviews.ui.overlays

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.models.enums.LocationType
import com.neilturner.aerialviews.models.prefs.InterfacePrefs
import com.neilturner.aerialviews.models.videos.AerialVideo
import com.neilturner.aerialviews.ui.screensaver.ExoPlayerView

class TextLocation : AppCompatTextView {
    // replace with https://juliensalvi.medium.com/safe-delay-in-android-views-goodbye-handlers-hello-coroutines-cd47f53f0fbf
    private var currentPositionProgressHandler: (() -> Unit)? = null
    private val textAlpha = 1f // start + end values?
    private var canSkip = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        TextViewCompat.setTextAppearance(this, R.style.LocationText)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentPositionProgressHandler = null
    }

    fun updateLocationData(location: String, poi: Map<Int, String>, locationType: LocationType, player: ExoPlayerView) {
        // If POI, set POI text, if empty use location, or else use location
        this.text = if (locationType == LocationType.POI) {
            poi[0]?.replace("\n", " ") ?: location
        } else {
            location
        }

        // Hide TextView if POI/location text is blank, or Location is set to off
        if (this.text.isBlank()) {
            this.visibility = View.GONE
        } else if (locationType != LocationType.OFF) {
            this.visibility = View.VISIBLE
        }

        // If set to POI, set timer to update text when interval is reached
        if (locationType == LocationType.POI && poi.size > 1) { // everything else is static anyways
            updatePointsOfInterest(poi, player)
        } else {
            // POI is off or empty, so disable handler
            currentPositionProgressHandler = null
        }

        // 4. Is a location is visible and text is LTR (eg. Arabic), set text direction
        // May not be needed as text alignment can be used?
//        if (InterfacePrefs.locationStyle != LocationType.OFF &&
//            this.text.isNotBlank()
//        ) {
//            if (LocaleHelper.isLtrText(this.text.toStringOrEmpty())) {
//                videoBinding.clock.textDirection = View.TEXT_DIRECTION_LTR
//            } else {
//                videoBinding.clock.textDirection = View.TEXT_DIRECTION_LOCALE
//            }
//        }
    }

    private fun updatePointsOfInterest(poi: Map<Int, String>, player: ExoPlayerView) {
        val poiTimes = poi.keys.sorted() // sort ahead of time?
        var lastPoi = 0

        currentPositionProgressHandler = {
            // Find POI string at current position/time
            val time = player.currentPosition / 1000 // player current position
            val currentPoi = poiTimes.findLast { it <= time } ?: 0
            val update = currentPoi != lastPoi

            // If new string and not fading in/out + loading new video
            if (update && canSkip) {
                // Set new string and fade in
                lastPoi = currentPoi
                this.animate().alpha(0f).setDuration(1000).withEndAction {
                    this.text = poi[currentPoi]?.replace("\n", " ")
                    this.animate().alpha(textAlpha).setDuration(1000).start()
                }.start()
            }

            // Set new interval for POI string update
            // Longer is a new string has just been set
            val interval = if (update) 3000 else 1000 // Small change to make ktlint happy
            this.postDelayed({
                currentPositionProgressHandler?.let { it() }
            }, interval.toLong())
        }

        // Setup handler for initial run of this video
        this.postDelayed({
            currentPositionProgressHandler?.let { it() }
        }, 1000)
    }

    companion object {
        private const val TAG = "TextLocation"
    }
}
