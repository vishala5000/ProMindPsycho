# Quote Video Generator Android

Native Android Kotlin app. No Python is used by the Android application.

Features:
- Bulk one-quote-per-line generation
- 1080x1920 vertical video
- Exactly 8 seconds
- H.264/AVC using libx264
- bg.mp3 looped for the full 8 seconds
- 700x700 text wrapping area
- Text centered in the 1080x1920 frame
- Slightly smaller font: 72px maximum, 24px minimum
- Saves MP4 files to Movies/QuoteVideoGenerator
- Saves videos.zip to Downloads/QuoteVideoGenerator
- GitHub Actions debug APK build

Assets required:
app/src/main/assets/bg.png
app/src/main/assets/font.ttf
app/src/main/assets/bg.mp3

FFmpeg dependency:
dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.7

Note: the GPL package is used because the requested FFmpeg command uses libx264. Review the package's GPL licensing obligations before distributing the APK.
