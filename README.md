# VideoTube – offline personal video library (Kotlin + Jetpack Compose)

Build the APK
1. Install Android Studio (Koala or newer, JDK 17). File > Open > this folder. Let Gradle sync.
2. Build > Build APK(s). Output: app/build/outputs/apk/debug/app-debug.apk
   (CLI: run `gradle wrapper --gradle-version 8.7` once, then `./gradlew assembleDebug`)
3. Copy the APK to your phone and install (allow "install unknown apps"), or press Run with a device attached.

Notes
- Fully offline: Room (SQLite) for metadata; videos, thumbnails and avatars are copied to app-private storage.
- No permissions requested: the system photo picker / file picker grants access to the files you choose.
- minSdk 24 (Android 7.0+).
