package com.lucasvinicius.musicwallpaper.data.repository

import com.lucasvinicius.musicwallpaper.data.model.LookupResult
import com.lucasvinicius.musicwallpaper.data.model.TrackInfo

interface ArtworkRepository {
    suspend fun resolveArtwork(trackInfo: TrackInfo): LookupResult
}