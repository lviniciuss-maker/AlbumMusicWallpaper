package com.lucasvinicius.musicwallpaper.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lucasvinicius.musicwallpaper.App
import com.lucasvinicius.musicwallpaper.data.local.StaticArtworkStorage
import com.lucasvinicius.musicwallpaper.data.model.LookupResult
import com.lucasvinicius.musicwallpaper.data.model.WallpaperContent
import com.lucasvinicius.musicwallpaper.data.model.WallpaperContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MusicNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser by lazy { NotificationMediaParser(this) }

    private var pauseTimerJob: Job? = null
    private var lastProcessedTrackKey: String? = null
    private var lastSavedContent: WallpaperContent? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in SupportedMusicApps.packages) return

        val notification = sbn.notification ?: return
        val trackInfo = parser.parse(notification, packageName) ?: return

        val app = application as App
        val staticArtworkStorage = StaticArtworkStorage(applicationContext)

        val trackKey = "${trackInfo.title}-${trackInfo.artist}"

        if (trackInfo.isPlaying) {
            pauseTimerJob?.cancel()

            if (trackKey == lastProcessedTrackKey && lastSavedContent != null) {
                serviceScope.launch {
                    app.wallpaperStateStore.save(lastSavedContent!!)
                }
                return
            }

            serviceScope.launch {
                val finalContent = when (val result = app.artworkRepository.resolveArtwork(trackInfo)) {
                    is LookupResult.Success -> {
                        // A GRANDE CORREÇÃO ESTÁ AQUI: Guardamos a foto estática no bolso!
                        val backupPath = trackInfo.staticArtworkBitmap?.let {
                            staticArtworkStorage.save(it, trackInfo)
                        }

                        WallpaperContent(
                            trackTitle = trackInfo.title,
                            trackArtist = trackInfo.artist,
                            trackAlbum = trackInfo.album,
                            sourcePackage = trackInfo.packageName,
                            contentType = WallpaperContentType.ANIMATED,
                            animatedUrl = result.artwork.hlsUrl,
                            staticImagePath = backupPath, // <-- AGORA TEMOS MUNIÇÃO PARA O ESPIÃO!
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    is LookupResult.StaticHighRes -> {
                        val path = staticArtworkStorage.downloadAndSave(result.imageUrl, trackInfo)
                        if (path != null) {
                            WallpaperContent(trackTitle = trackInfo.title, trackArtist = trackInfo.artist, trackAlbum = trackInfo.album, sourcePackage = trackInfo.packageName, contentType = WallpaperContentType.STATIC, animatedUrl = null, staticImagePath = path, updatedAt = System.currentTimeMillis())
                        } else {
                            getPoorQualityFallback(trackInfo, app, staticArtworkStorage)
                        }
                    }
                    else -> getPoorQualityFallback(trackInfo, app, staticArtworkStorage)
                }

                lastProcessedTrackKey = trackKey
                lastSavedContent = finalContent
                app.wallpaperStateStore.save(finalContent)
            }
        }
        else {
            pauseTimerJob?.cancel()
            pauseTimerJob = serviceScope.launch {
                delay(5000)
                applyDefaultWallpaper(app)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName !in SupportedMusicApps.packages) return
        pauseTimerJob?.cancel()
        serviceScope.launch {
            val app = application as App
            applyDefaultWallpaper(app)
        }
    }

    private suspend fun applyDefaultWallpaper(app: App) {
        val defaultImagePath = app.wallpaperStateStore.defaultWallpaperFlow.first()
        if (defaultImagePath != null) {
            app.wallpaperStateStore.save(WallpaperContent(trackTitle = "Música Pausada", contentType = WallpaperContentType.STATIC, staticImagePath = defaultImagePath, updatedAt = System.currentTimeMillis()))
        } else {
            app.wallpaperStateStore.save(WallpaperContent(contentType = WallpaperContentType.NONE, updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun getPoorQualityFallback(trackInfo: com.lucasvinicius.musicwallpaper.data.model.TrackInfo, app: App, storage: StaticArtworkStorage): WallpaperContent {
        val bitmap = trackInfo.staticArtworkBitmap
        return if (bitmap != null) {
            val path = storage.save(bitmap, trackInfo)
            WallpaperContent(trackTitle = trackInfo.title, trackArtist = trackInfo.artist, trackAlbum = trackInfo.album, sourcePackage = trackInfo.packageName, contentType = WallpaperContentType.STATIC, animatedUrl = null, staticImagePath = path, updatedAt = System.currentTimeMillis())
        } else {
            val currentState = app.wallpaperStateStore.contentFlow.first()
            if (currentState.trackTitle == trackInfo.title && currentState.contentType == WallpaperContentType.STATIC) {
                currentState
            } else {
                WallpaperContent(trackTitle = trackInfo.title, trackArtist = trackInfo.artist, trackAlbum = trackInfo.album, sourcePackage = trackInfo.packageName, contentType = WallpaperContentType.NONE, animatedUrl = null, staticImagePath = null, updatedAt = System.currentTimeMillis())
            }
        }
    }
}