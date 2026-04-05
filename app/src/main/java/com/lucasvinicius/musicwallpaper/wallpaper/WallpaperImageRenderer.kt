package com.lucasvinicius.musicwallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder

class WallpaperImageRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Adicionamos o parâmetro dimPercentage com padrão 0
    fun drawFromPath(holder: SurfaceHolder, path: String, dimPercentage: Int = 0) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        draw(holder, bitmap, dimPercentage)
    }

    fun draw(holder: SurfaceHolder, bitmap: Bitmap, dimPercentage: Int = 0) {
        val canvas = holder.lockCanvas() ?: return
        try {
            // Pinta o fundo de preto
            canvas.drawColor(Color.BLACK)

            val src = Rect(0, 0, bitmap.width, bitmap.height)
            val dst = calculateCenterCropRect(
                bitmap.width,
                bitmap.height,
                canvas.width,
                canvas.height
            )

            // 1. Desenha a foto da capa centralizada e expandida
            canvas.drawBitmap(bitmap, src, dst, paint)

            // 2. A MÁGICA ACONTECE AQUI: Aplica o "insulfilm" se necessário
            if (dimPercentage > 0) {
                // Garante que o valor fique entre 0 e 100, e converte para Alpha (0 a 255)
                val safeDim = dimPercentage.coerceIn(0, 100)
                val alpha = (safeDim / 100f * 255).toInt()

                val dimPaint = Paint().apply {
                    color = Color.argb(alpha, 0, 0, 0)
                }

                // Desenha a película escura do tamanho exato da tela
                canvas.drawRect(
                    0f,
                    0f,
                    canvas.width.toFloat(),
                    canvas.height.toFloat(),
                    dimPaint
                )
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    // Matemática para a foto não ficar esticada ou achatada
    private fun calculateCenterCropRect(
        bitmapWidth: Int,
        bitmapHeight: Int,
        canvasWidth: Int,
        canvasHeight: Int
    ): Rect {
        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val canvasRatio = canvasWidth.toFloat() / canvasHeight.toFloat()

        return if (bitmapRatio > canvasRatio) {
            val scaledWidth = (canvasHeight * bitmapRatio).toInt()
            val left = (canvasWidth - scaledWidth) / 2
            Rect(left, 0, left + scaledWidth, canvasHeight)
        } else {
            val scaledHeight = (canvasWidth / bitmapRatio).toInt()
            val top = (canvasHeight - scaledHeight) / 2
            Rect(0, top, canvasWidth, top + scaledHeight)
        }
    }
}