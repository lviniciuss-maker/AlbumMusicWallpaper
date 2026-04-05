# 🎵 Music Wallpaper (Live)

Um aplicativo Android nativo que transforma a tela de bloqueio e a tela inicial do seu dispositivo em uma experiência musical imersiva. Ele detecta a música que está tocando atualmente no seu reprodutor favorito (Spotify, Apple Music, etc.) e aplica automaticamente a capa do álbum animada (Canvas) ou estática em alta resolução como papel de parede do sistema.

## ✨ Funcionalidades

 Sincronização em Tempo Real Utiliza `NotificationListenerService` para detectar mudanças de faixas instantaneamente em segundo plano.
 Vídeos Animados (Canvas) Reprodução fluida de vídeos HLS usando `Media3ExoPlayer` diretamente na superfície do papel de parede.
 Fallback Inteligente Se a faixa não possuir vídeo animado, o app busca a capa estática em altíssima resolução (via iTunes API).
 Controle de Escurecimento (Dimming) Um slider em tempo real permite ao usuário escurecer a capa estática (0% a 100%) para melhorar a legibilidade do relógio e notificações na tela de bloqueio.
 Modo OciosoPausado Permite escolher uma foto personalizada da galeria para ser exibida quando nenhuma música estiver tocando.

## 🛠️ Desafios Técnicos Resolvidos

Construir um Live Wallpaper contínuo exige lidar com fragmentação de hardware e peculiaridades do Android. Este projeto conta com soluções de arquitetura para problemas complexos

1. Tratamento de Codecs e HDR (Falso Negativo)
   Muitas APIs de música retornam vídeos codificados em perfis pesados, como HEVC 10-bits (HDR). Em segundo plano, o hardware de muitos dispositivos bloqueia a decodificação (`MediaCodecRenderer$DecoderInitializationException`). O app possui um Listener de erros acoplado ao ExoPlayer que intercepta essa falha de hardware em milissegundos e faz um fallback silencioso para a capa estática, impedindo travamentos e telas pretas.

2. Bypass de Bloqueio de Surface (OppoColorOS)
   Sistemas operacionais altamente modificados (como ColorOSRealmeUI) costumam travar o `SurfaceHolder` assim que um `Canvas` estático é desenhado (`cur=2 req=3 Invalid argument -22`). Para alternar perfeitamente entre uma Foto (Canvas) e um Vídeo (ExoPlayer) sem gerar crashes, implementei uma técnica de reconfiguração de formato de pixel (`PixelFormat.RGBX_8888` vs `RGBA_8888`), forçando o sistema a recriar a superfície dinamicamente de forma limpa.

3. Sanitização de Metadados para APIs
   Um algoritmo de limpeza de strings (Faxineiro de Nomes) foi implementado para remover sufixos inúteis inseridos por players (ex - EP, - Single, (feat. XYZ)). O repositório realiza um sistema de retry em 3 etapas (Busca Completa - Busca sem Faixa - Busca sem Álbum) para garantir a maior taxa de acerto possível nas APIs REST.

## 💻 Tecnologias e Bibliotecas Utilizadas

 Linguagem Kotlin
 Arquitetura Coroutines & Kotlin Flow (Reatividade)
 Persistência de Dados Jetpack DataStore (Preferences)
 Rede Retrofit2 & OkHttp3
 Mídia AndroidX Media3 (ExoPlayer & HLS)
 Componentes Core `WallpaperService`, `NotificationListenerService`
