package com.lucasvinicius.musicwallpaper.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import com.lucasvinicius.musicwallpaper.data.model.TrackInfo

class NotificationMediaParser(private val context: Context) {

    fun parse(notification: Notification, packageName: String): TrackInfo? {
        val extras = notification.extras ?: return null

        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        var album = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty().ifBlank { null }
        var bitmap = extractBitmapFromExtras(extras)

        // Agora assumimos que está pausado até provar o contrário
        var isPlaying = false

        try {
            val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            }

            if (token != null) {
                val mediaController = MediaController(context, token)
                val metadata = mediaController.metadata
                val playbackState = mediaController.playbackState

                // LÊ O ESTADO DE REPRODUÇÃO!
                if (playbackState != null) {
                    isPlaying = (playbackState.state == PlaybackState.STATE_PLAYING || playbackState.state == PlaybackState.STATE_BUFFERING)
                }

                if (metadata != null) {
                    if (album == null) album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim()?.ifBlank { null }
                    if (bitmap == null) bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    if (title.isBlank()) title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
                    if (artist.isBlank()) artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
                }
            }
        } catch (e: Exception) {}

        if (title.isBlank() || artist.isBlank()) return null

        return TrackInfo(
            title = title,
            artist = artist,
            album = album,
            packageName = packageName,
            isPlaying = isPlaying, // PASSAMOS O STATUS REAL AQUI!
            staticArtworkBitmap = bitmap
        )
    }

    private fun extractBitmapFromExtras(extras: Bundle): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java) ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            (extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap) ?: (extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
        }
    }
}