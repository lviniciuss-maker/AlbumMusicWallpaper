package com.lucasvinicius.musicwallpaper.data.model

data class ArtworkResponseDto(
    val url: String?,
    val artist: String?,
    val album: String?,
    val isCached: Boolean?
)