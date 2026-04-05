package com.lucasvinicius.musicwallpaper.data.repository

import com.lucasvinicius.musicwallpaper.data.model.ArtworkResult
import com.lucasvinicius.musicwallpaper.data.model.LookupResult
import com.lucasvinicius.musicwallpaper.data.model.TrackInfo
import com.lucasvinicius.musicwallpaper.data.remote.ArtworkApi
import com.lucasvinicius.musicwallpaper.data.remote.ItunesApi

class ArtworkRepositoryImpl(
    private val artworkApi: ArtworkApi,
    private val itunesApi: ItunesApi
) : ArtworkRepository {

    // O nosso "Faxineiro" de nomes para agradar a API
    private fun cleanText(text: String): String {
        return text.replace("🅴", "")
            .replace(Regex("(?i)\\s*\\(Explicit\\)"), "")
            .replace(Regex("(?i)\\s*\\[Explicit\\]"), "")
            .replace(Regex("(?i)\\s*-\\s*EP$"), "")
            .replace(Regex("(?i)\\s*-\\s*Single$"), "")
            .replace(Regex("(?i)\\s*\\(feat\\..*?\\)"), "")
            .trim()
    }

    override suspend fun resolveArtwork(trackInfo: TrackInfo): LookupResult {
        val cleanArtist = cleanText(trackInfo.artist)
        val cleanTitle = cleanText(trackInfo.title)
        val cleanAlbum = trackInfo.album?.let { cleanText(it) }?.ifBlank { null } ?: cleanTitle

        if (cleanArtist.isBlank()) {
            return LookupResult.Error("Metadados insuficientes: artista obrigatório.")
        }

        // --- TENTATIVAS INTELIGENTES NA API DE VÍDEO ---

        // Tentativa 1: Busca Completa
        var m8Result = fetchM8tec(cleanArtist, cleanAlbum, cleanTitle)
        if (m8Result is LookupResult.Success) return m8Result

        // Tentativa 2: Busca só pelo Álbum (Muitas vezes a Apple acha mais fácil sem a faixa)
        m8Result = fetchM8tec(cleanArtist, cleanAlbum, null)
        if (m8Result is LookupResult.Success) return m8Result

        // Tentativa 3: Se for um Single (álbum igual à música), tenta buscar sem álbum
        if (cleanAlbum != cleanTitle) {
            m8Result = fetchM8tec(cleanArtist, cleanTitle, null)
            if (m8Result is LookupResult.Success) return m8Result
        }

        // --- PLANO B: ITUNES (FOTO 4K ESTÁTICA) ---
        return try {
            val term = "$cleanArtist $cleanTitle"
            val itunesResponse = itunesApi.searchTrack(term = term)

            if (itunesResponse.isSuccessful) {
                val result = itunesResponse.body()?.results?.firstOrNull()
                val thumbnailUrl = result?.artworkUrl100

                if (thumbnailUrl != null) {
                    val highResUrl = thumbnailUrl.replace("100x100bb", "1000x1000bb")
                    LookupResult.StaticHighRes(highResUrl)
                } else {
                    LookupResult.NotFound
                }
            } else {
                LookupResult.NotFound
            }
        } catch (e: Exception) {
            LookupResult.Error(e.message ?: "Erro no iTunes")
        }
    }

    // Função ajudante para não repetir código
    private suspend fun fetchM8tec(artist: String, album: String, title: String?): LookupResult {
        return try {
            val response = artworkApi.searchArtwork(
                artist = artist,
                album = album,
                title = title?.ifBlank { null }
            )

            if (response.isSuccessful) {
                val body = response.body()
                val url = body?.url

                if (!url.isNullOrBlank()) {
                    LookupResult.Success(
                        ArtworkResult(
                            hlsUrl = url,
                            artist = body.artist ?: artist,
                            album = body.album ?: album,
                            isCached = body.isCached ?: false
                        )
                    )
                } else LookupResult.NotFound
            } else {
                LookupResult.NotFound
            }
        } catch (e: Exception) {
            LookupResult.Error(e.message ?: "Erro de conexão API M8TEC")
        }
    }
}