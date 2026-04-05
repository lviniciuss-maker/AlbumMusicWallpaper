package com.lucasvinicius.musicwallpaper.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.lucasvinicius.musicwallpaper.App
import com.lucasvinicius.musicwallpaper.data.model.WallpaperContent
import com.lucasvinicius.musicwallpaper.data.model.WallpaperContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class AnimatedWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AnimatedWallpaperEngine()
    }

    inner class AnimatedWallpaperEngine : Engine() {

        private val engineScope = CoroutineScope(Dispatchers.Main)
        private lateinit var playerManager: WallpaperPlayerManager
        private val imageRenderer = WallpaperImageRenderer()

        private var observeContentJob: Job? = null
        private var observeDimJob: Job? = null

        private var currentHolder: SurfaceHolder? = null
        private var surfaceReady = false
        private var lastContent: WallpaperContent? = null

        // A nossa memória para saber se o Oppo "soldou" a tela com a foto
        private var wasUsingCanvas = false

        // Memória do nível de escurecimento atual
        private var currentDimLevel = 30

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            playerManager = WallpaperPlayerManager(applicationContext) {
                // PLANO B: O vídeo falhou! Mudamos a música para o formato STATIC e salvamos.
                val content = lastContent ?: return@WallpaperPlayerManager
                val app = application as App

                if (content.contentType == WallpaperContentType.ANIMATED && content.staticImagePath != null) {
                    engineScope.launch {
                        app.wallpaperStateStore.save(
                            content.copy(contentType = WallpaperContentType.STATIC, animatedUrl = null)
                        )
                    }
                }
            }

            val app = application as App

            // 1. Observa a música que está tocando
            observeContentJob = engineScope.launch {
                app.wallpaperStateStore.contentFlow.collectLatest { content ->
                    lastContent = content
                    renderContent(content)
                }
            }

            // 2. Observa a barra de escurecimento (Slider)
            observeDimJob = engineScope.launch {
                app.wallpaperStateStore.dimLevelFlow.collectLatest { level ->
                    currentDimLevel = level
                    // Sempre que o nível muda, forçamos a tela a pintar o novo escurecimento
                    lastContent?.let { renderContent(it) }
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            surfaceReady = true
            playerManager.attachSurface(holder)

            // A tela nova e destravada chegou! Vamos desenhar.
            lastContent?.let { renderContent(it) }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            currentHolder = holder
            lastContent?.let { renderContent(it) }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            val content = lastContent ?: return

            when {
                visible && content.contentType == WallpaperContentType.ANIMATED -> playerManager.resume()
                !visible && content.contentType == WallpaperContentType.ANIMATED -> playerManager.pause()
                visible && content.contentType == WallpaperContentType.STATIC -> renderContent(content)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            surfaceReady = false
            currentHolder = null
            playerManager.detachSurface()
        }

        override fun onDestroy() {
            super.onDestroy()
            observeContentJob?.cancel()
            observeDimJob?.cancel()
            playerManager.release()
            engineScope.cancel()
        }

        private fun renderContent(content: WallpaperContent) {
            val holder = currentHolder ?: return

            when (content.contentType) {
                WallpaperContentType.ANIMATED -> {
                    // O HACK DO OPPO: Se usávamos foto, a tela está bloqueada. Vamos destruí-la!
                    if (wasUsingCanvas) {
                        wasUsingCanvas = false
                        val w = holder.surfaceFrame.width()
                        val h = holder.surfaceFrame.height()
                        if (w > 0 && h > 0) {
                            holder.setFixedSize(w, h - 1) // Encolhe a tela 1 pixel
                            holder.setSizeFromLayout()    // Volta ao tamanho original
                        }
                        // Saímos daqui e esperamos o Android chamar onSurfaceCreated com a tela nova
                        return
                    }

                    if (!surfaceReady) return

                    val url = content.animatedUrl ?: return
                    playerManager.attachSurface(holder)
                    playerManager.play(url)
                }

                WallpaperContentType.STATIC -> {
                    wasUsingCanvas = true // Avisamos que soldamos a tela com o cabo de foto

                    if (!surfaceReady) return
                    val path = content.staticImagePath ?: return
                    if (!File(path).exists()) return

                    playerManager.stopAndClearSurface()

                    // ENVIAMOS O NÍVEL DE ESCURECIMENTO AQUI:
                    imageRenderer.drawFromPath(holder, path, currentDimLevel)
                }

                WallpaperContentType.NONE -> {}
            }
        }
    }
}