# SQLCipher / zetetic
-keep class net.zetetic.** { *; }
-keep interface net.zetetic.** { *; }

# Room generated
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Hilt generated components (handled by plugin, but safe)
-keep class dagger.hilt.** { *; }
