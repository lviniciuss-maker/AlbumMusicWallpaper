# 🎵 Music Wallpaper (Live)

A native Android application that transforms your device's lock screen and home screen into an immersive musical experience. It detects the currently playing track from your favorite media player (Spotify, Apple Music, etc.) and automatically applies the animated album cover (Canvas) or high-resolution static artwork as the system wallpaper.

## ✨ Features

* **Real-Time Synchronization:** Uses `NotificationListenerService` to detect track changes instantly in the background.
* **Animated Videos (Canvas):** Smooth playback of HLS videos using `Media3/ExoPlayer` directly on the wallpaper surface.
* **Smart Fallback:** If the track doesn't have an animated video, the app fetches high-resolution static artwork (via iTunes API).
* **Dimming Control:** A real-time slider allows users to dim the static artwork (0% to 100%) to improve the readability of the clock and notifications on the lock screen.
* **Idle/Paused Mode:** Allows choosing a custom photo from the gallery to be displayed when no music is playing.

## 🛠️ Technical Challenges Resolved

Building a continuous Live Wallpaper requires dealing with hardware fragmentation and Android OS quirks. This project features architectural solutions for complex problems:

1. **Codec Handling and HDR (False Negatives):**
   Many music APIs return videos encoded in heavy profiles, such as 10-bit HEVC (HDR). Running in the background, the hardware of many devices blocks this decoding (`MediaCodecRenderer$DecoderInitializationException`). The app features an error *Listener* coupled to ExoPlayer that intercepts this hardware failure in milliseconds and makes a silent fallback to the static artwork, preventing crashes and black screens.

2. **Surface Lock Bypass (Oppo/ColorOS):**
   Highly modified operating systems (like ColorOS/RealmeUI) tend to "lock" the `SurfaceHolder` once a static `Canvas` is drawn (`cur=2 req=3 Invalid argument -22`). To seamlessly switch between a Photo (Canvas) and a Video (ExoPlayer) without causing crashes, I implemented a pixel format reconfiguration technique (`PixelFormat.RGBX_8888` vs `RGBA_8888`), forcing the OS to dynamically and cleanly recreate the surface.

3. **Metadata Sanitization for APIs:**
   A string cleaning algorithm was implemented to remove useless suffixes appended by media players (e.g., "- EP", "- Single", "(feat. XYZ)"). The repository performs a 3-step retry system (Full Search -> Search without Track -> Search without Album) to ensure the highest possible hit rate on REST APIs.

## 💻 Tech Stack

* **Language:** Kotlin
* **Architecture:** Coroutines & Kotlin Flow (Reactivity)
* **Local Storage:** Jetpack DataStore (Preferences)
* **Networking:** Retrofit2 & OkHttp3
* **Media:** AndroidX Media3 (ExoPlayer & HLS)
* **Core Components:** `WallpaperService`, `NotificationListenerService`
