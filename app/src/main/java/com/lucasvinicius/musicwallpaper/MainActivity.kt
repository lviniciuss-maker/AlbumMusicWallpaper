package com.lucasvinicius.musicwallpaper

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lucasvinicius.musicwallpaper.notification.MusicNotificationListenerService
import com.lucasvinicius.musicwallpaper.wallpaper.AnimatedWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var trackText: TextView
    private lateinit var artworkText: TextView

    // Nossas views clássicas para o controle de escurecimento
    private lateinit var dimLabel: TextView
    private lateinit var dimSeekBar: SeekBar

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val file = File(filesDir, "default_wallpaper.jpg")
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        file.absolutePath
                    } catch (e: Exception) {
                        null
                    }
                }

                if (savedPath != null) {
                    val app = application as App
                    app.wallpaperStateStore.saveDefaultWallpaper(savedPath)
                    Toast.makeText(this@MainActivity, "Papel de parede padrão salvo!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply { textSize = 16f }
        trackText = TextView(this).apply { textSize = 14f }
        artworkText = TextView(this).apply { textSize = 12f }

        // Texto da Barra de Escurecimento
        dimLabel = TextView(this).apply {
            text = "Escurecimento da Capa Estática: 30%"
            textSize = 16f
            setPadding(0, 32, 0, 8)
        }

        // A Barra Deslizante Clássica do Android (SeekBar)
        dimSeekBar = SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        dimLabel.text = "Escurecimento da Capa Estática: $progress%"
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Salva no banco de dados apenas quando soltar o dedo
                    seekBar?.let {
                        lifecycleScope.launch {
                            (application as App).wallpaperStateStore.saveDimLevel(it.progress)
                        }
                    }
                }
            })
        }

        val btn1 = Button(this).apply {
            text = "1. Abrir acesso às notificações"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }

        val btn2 = Button(this).apply {
            text = "2. Escolher este papel de parede (Live)"
            setOnClickListener {
                val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, AnimatedWallpaperService::class.java))
                }
                startActivity(intent)
            }
        }

        val btn3 = Button(this).apply {
            text = "3. Escolher Foto Padrão (Fundo para Pause/Ocioso)"
            setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(statusText)
            addView(btn1)
            addView(btn2)
            addView(btn3)

            // Adicionando os controles de escurecimento na tela
            addView(dimLabel)
            addView(dimSeekBar)

            addView(TextView(this@MainActivity).apply {
                text = "\n--- STATUS AO VIVO ---"
                textSize = 16f
                setPadding(0, 16, 0, 8)
            })
            addView(trackText)
            addView(artworkText)
        }

        setContentView(layout)
        updateNotificationAccessStatus()

        val app = application as App

        // Atualiza os dados da música
        lifecycleScope.launch {
            app.wallpaperStateStore.contentFlow.collect { content ->
                trackText.text = "\nFaixa: ${content.trackTitle ?: "-"}\nArtista: ${content.trackArtist ?: "-"}\nÁlbum: ${content.trackAlbum ?: "-"}\nTipo: ${content.contentType.name}"
                artworkText.text = "Animated URL: ${content.animatedUrl ?: "-"}\nStatic Path: ${content.staticImagePath ?: "-"}"
            }
        }

        // Atualiza a posição da barra de acordo com o que está salvo
        lifecycleScope.launch {
            app.wallpaperStateStore.dimLevelFlow.collect { level ->
                dimSeekBar.progress = level
                dimLabel.text = "Escurecimento da Capa Estática: $level%"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationAccessStatus()
    }

    private fun updateNotificationAccessStatus() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        val enabled = enabledListeners.contains(ComponentName(this, MusicNotificationListenerService::class.java).flattenToString())
        statusText.text = if (enabled) "✅ Acesso às notificações: PERMITIDO\n" else "❌ Acesso às notificações: BLOQUEADO\n"
    }
}