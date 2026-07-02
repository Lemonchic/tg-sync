# Karoo Music Player - ProGuard Rules

# Keep Karoo Extension service (discovered via manifest intent-filter)
-keep class com.example.karoo.music.extension.KarooMusicExtension { *; }

# Keep BroadcastReceiver for PendingIntent actions
-keep class com.example.karoo.music.extension.PlaybackActionReceiver { *; }

# Keep Room entities and DAOs
-keep class com.example.karoo.music.data.** { *; }

# Keep PlaybackService (foreground service)
-keep class com.example.karoo.music.playback.PlaybackService { *; }

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# AndroidX Security / Tink
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
