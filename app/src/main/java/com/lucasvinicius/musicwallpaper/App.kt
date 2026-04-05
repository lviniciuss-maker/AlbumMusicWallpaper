package com.lucasvinicius.musicwallpaper

import android.app.Application
import com.lucasvinicius.musicwallpaper.data.local.WallpaperStateStore
import com.lucasvinicius.musicwallpaper.data.remote.ArtworkApi
import com.lucasvinicius.musicwallpaper.data.remote.ItunesApi
import com.lucasvinicius.musicwallpaper.data.repository.ArtworkRepository
import com.lucasvinicius.musicwallpaper.data.repository.ArtworkRepositoryImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {

    lateinit var artworkRepository: ArtworkRepository
        private set

    lateinit var wallpaperStateStore: WallpaperStateStore
        private set

    override fun onCreate() {
        super.onCreate()

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val okHttpClient = OkHttpClient.Builder().addInterceptor(logging).build()

        // 1. Conexão com a M8TEC (Vídeos)
        val retrofitM8tec = Retrofit.Builder()
            .baseUrl("https://artwork.m8tec.top/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val artworkApi = retrofitM8tec.create(ArtworkApi::class.java)

        // 2. Conexão com o iTunes (Fotos 4K)
        val retrofitItunes = Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val itunesApi = retrofitItunes.create(ItunesApi::class.java)

        wallpaperStateStore = WallpaperStateStore(this)

        // Entregamos as duas conexões pro nosso Repositório trabalhar!
        artworkRepository = ArtworkRepositoryImpl(artworkApi, itunesApi)
    }
}