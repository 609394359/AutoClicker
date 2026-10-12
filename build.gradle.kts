// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.0" apply false
    // Kotlin 2.3.21 is the minimum KGP version with full Gradle 9.2 support
    // (KGP 1.9.x / 2.2.x only support Gradle up to 8.14 and crash on 9.x).
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    // Kotlin 2.x Compose: the compiler ships as a Gradle plugin (replaces kotlinCompilerExtensionVersion).
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
