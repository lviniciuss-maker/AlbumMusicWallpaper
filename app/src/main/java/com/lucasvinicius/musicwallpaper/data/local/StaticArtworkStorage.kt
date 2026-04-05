package com.lucasvinicius.musicwallpaper.data.local

import android.content.Context
import android.graphics.Bitmap
import com.lucasvinicius.musicwallpaper.data.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class StaticArtworkStorage(
    private val context: Context
) {
    // Salva a foto ruim do Android (Plano C)
    suspend fun save(bitmap: Bitmap, trackInfo: TrackInfo): String = withContext(Dispatchers.IO) {
        val safeArtist = sanitize(trackInfo.artist)
        val safeTitle = sanitize(trackInfo.title)
        val file = File(context.cacheDir, "art_${safeArtist}_${safeTitle}.jpg")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
        }
        file.absolutePath
    }

    // BAIXA A FOTO 4K DA INTERNET (Plano B)
    suspend fun downloadAndSave(imageUrl: String, trackInfo: TrackInfo): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = URL(imageUrl).readBytes()
            val safeArtist = sanitize(trackInfo.artist)
            val safeTitle = sanitize(trackInfo.title)
            val file = File(context.cacheDir, "highres_${safeArtist}_${safeTitle}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            null // Se der erro de internet, devolve nulo
        }
    }

    private fun sanitize(value: String): String {
        return value.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}