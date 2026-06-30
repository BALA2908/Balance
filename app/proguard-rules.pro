# SQLCipher / zetetic
-keep class net.zetetic.** { *; }
-keep interface net.zetetic.** { *; }

# Room generated
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Hilt generated components (handled by plugin, but safe)
-keep class dagger.hilt.** { *; }

# ML Kit — on-device GenAI (Gemini Nano) + receipt text recognition. Reflection /
# dynamic feature loading; keep defensively so R8 never strips a used path.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# WorkManager workers are instantiated reflectively (HiltWorkerFactory handles
# ours, but keep the base to be safe).
-keep class * extends androidx.work.ListenableWorker { *; }
