package com.neilturner.aerialviews.ui.core

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import com.neilturner.aerialviews.models.enums.AerialMediaSource
import com.neilturner.aerialviews.models.enums.LimitLongerVideos
import com.neilturner.aerialviews.models.prefs.GeneralPrefs
import com.neilturner.aerialviews.models.videos.AerialMedia
import com.neilturner.aerialviews.services.CustomRendererFactory
import com.neilturner.aerialviews.services.SambaDataSourceFactory
import com.neilturner.aerialviews.services.WebDavDataSourceFactory
import com.neilturner.aerialviews.utils.WindowHelper
import timber.log.Timber
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

object VideoPlayerHelper {
    fun setRefreshRate(
        context: Context,
        framerate: Float?,
    ) {
        if (framerate == null || framerate == 0f) {
            Timber.i("Unable to get video frame rate...")
            return
        }

        Timber.i("${framerate}fps video, setting refresh rate if needed...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowHelper.setLegacyRefreshRate(context, framerate)
        }
    }

    fun disableAudioTrack(player: ExoPlayer) {
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
    }

    @OptIn(UnstableApi::class)
    fun buildPlayer(context: Context): ExoPlayer {
        val parametersBuilder = DefaultTrackSelector.Parameters.Builder(context)

        if (GeneralPrefs.enableTunneling) {
            parametersBuilder
                .setTunnelingEnabled(true)
        }

        val trackSelector = DefaultTrackSelector(context)
        trackSelector.parameters = parametersBuilder.build()

        var rendererFactory = DefaultRenderersFactory(context)
        if (GeneralPrefs.allowFallbackDecoders) {
            rendererFactory.setEnableDecoderFallback(true)
        }
        if (GeneralPrefs.philipsDolbyVisionFix) {
            rendererFactory = CustomRendererFactory(context)
        }

        val player =
            ExoPlayer
                .Builder(context)
                .setTrackSelector(trackSelector)
                .setRenderersFactory(rendererFactory)
                .build()

        if (GeneralPrefs.enablePlaybackLogging) {
            player.addAnalyticsListener(EventLogger())
        }

        if (!GeneralPrefs.muteVideos) player.volume = GeneralPrefs.videoVolume.toFloat() / 100 else player.volume = 0f

        // https://medium.com/androiddevelopers/prep-your-tv-app-for-android-12-9a859d9bb967
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && GeneralPrefs.refreshRateSwitching) {
            Timber.i("Android 12+, enabling refresh rate switching")
            player.videoChangeFrameRateStrategy = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
        }

        player.setPlaybackSpeed(GeneralPrefs.playbackSpeed.toFloat())
        return player
    }

    @OptIn(UnstableApi::class)
    fun setupMediaSource(
        player: ExoPlayer,
        media: AerialMedia,
    ) {
        val mediaItem = MediaItem.fromUri(media.uri)
        when (media.source) {
            AerialMediaSource.SAMBA -> {
                val mediaSource =
                    ProgressiveMediaSource
                        .Factory(SambaDataSourceFactory())
                        .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
            }
            AerialMediaSource.WEBDAV -> {
                val mediaSource =
                    ProgressiveMediaSource
                        .Factory(WebDavDataSourceFactory())
                        .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
            }
            else -> {
                player.setMediaItem(mediaItem)
            }
        }
    }

    fun calculateSegments(video: VideoInfo) {
        // 10 seconds is the min. video length
        val tenSeconds = 10 * 1000
        if (video.maxLength < tenSeconds) {
            video.isSegmented = false
            video.segmentStart = 0L
            video.segmentEnd = 0L
            return
        }

        // If less than 2 segments
        val segments = video.duration / video.maxLength
        if (segments < 2) {
            video.isSegmented = false
            video.segmentStart = 0L
            video.segmentEnd = 0L
            return
        }

        val length = video.duration.floorDiv(segments).toLong()
        val random = (1..segments).random()
        val segmentStart = (random - 1) * length
        val segmentEnd = random * length

        val message1 = "Segment chosen: ${segmentStart.milliseconds} - ${segmentEnd.milliseconds}"
        val message2 = "(video is ${video.duration.milliseconds}, Segments: $segments)"
        Timber.i("$message1 $message2")

        video.isSegmented = true
        video.segmentStart = segmentStart
        video.segmentEnd = segmentEnd
    }

    fun calculateDelay(
        video: VideoInfo,
        player: ExoPlayer,
        prefs: GeneralPrefs,
    ): Long {
        val loopShortVideos = prefs.loopShortVideos
        val allowLongerVideos = prefs.limitLongerVideos == LimitLongerVideos.IGNORE
        video.duration = player.duration

        // 10 seconds is the min. video length
        val tenSeconds = 10 * 1000

        // If max length disabled, play full video
        if (video.maxLength < tenSeconds) {
            return calculateEndOfVideo(video, prefs, player.currentPosition)
        }

        // Play a part/segment of a video only
        if (video.isSegmented) {
            val position = if (player.currentPosition < video.segmentStart) 0 else player.currentPosition - video.segmentStart
            video.duration = video.segmentEnd - video.segmentStart
            return calculateEndOfVideo(video, prefs, position)
        }

        // Check if we need to loop the video
        if (loopShortVideos &&
            video.duration < video.maxLength
        ) {
            val (isLooping, duration) = calculateLoopingVideo(video, player)
            if (isLooping) {
                player.repeatMode = Player.REPEAT_MODE_ALL
                video.duration = duration
            }
            val position = (video.loopCount * video.duration) + player.currentPosition
            return calculateEndOfVideo(video, prefs, position)
        }

        // Limit the duration of the video, or not
        if (video.maxLength in tenSeconds until video.duration &&
            !allowLongerVideos
        ) {
            Timber.i("Limiting duration (video is ${video.duration.milliseconds}, limit is ${video.maxLength.milliseconds})")
            return calculateEndOfVideo(video, prefs, player.currentPosition)
        }
        Timber.i("Ignoring limit (video is ${video.duration.milliseconds}, limit is ${video.maxLength.milliseconds})")
        return calculateEndOfVideo(video, prefs, player.currentPosition)
    }

    private fun calculateEndOfVideo(
        video: VideoInfo,
        prefs: GeneralPrefs,
        position: Long,
    ): Long {
        // Adjust the duration based on the playback speed
        // Take into account the current player position in case of speed changes during playback
        val delay = ((video.duration - position) / prefs.playbackSpeed.toDouble().toLong() - prefs.mediaFadeOutDuration.toLong())
        val actualPosition = if (video.isSegmented) position + video.segmentStart else position
        Timber.i("Delay: ${delay.milliseconds} (Duration: ${video.duration.milliseconds}, Position: ${actualPosition.milliseconds})")
        return if (delay < 0) 0 else delay
    }

    private fun calculateLoopingVideo(
        video: VideoInfo,
        player: ExoPlayer,
    ): Pair<Boolean, Long> {
        val loopCount = ceil(video.maxLength / player.duration.toDouble()).toInt()
        val targetDuration = player.duration * loopCount
        Timber.i("Looping $loopCount times (video is ${player.duration.milliseconds}, limit is ${video.maxLength.milliseconds})")
        return Pair(loopCount > 1, targetDuration.toLong())
    }
}
