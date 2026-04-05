package com.lucasvinicius.musicwallpaper.data.model

sealed class LookupResult {
    data class Success(val artwork: ArtworkResult) : LookupResult()
    data class StaticHighRes(val imageUrl: String) : LookupResult() // NOSSA NOVIDADE!
    data object NotFound : LookupResult()
    data class Error(val message: String) : LookupResult()
}