# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Keep accessibility service entry points (referenced by AndroidManifest)
-keep class com.autoclicker.app.service.AutoClickService { *; }

# Keep model data classes (kotlin data class synthetic names)
-keepclassmembers class com.autoclicker.app.model.** { *; }

# Keep R
-keep class com.autoclicker.app.R$* { *; }

-dontwarn org.jetbrains.annotations.**
