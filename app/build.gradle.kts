plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.x: Compose compiler is now a Gradle plugin; kotlinCompilerExtensionVersion is gone.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.autoclicker.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autoclicker.app"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Default to debug signing so `./gradlew assembleRelease` produces a usable APK.
            // For production, place your own keystore.jks under keystore/ and uncomment the block below.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // NOTE: with the Kotlin 2.x Compose Gradle plugin, composeOptions / kotlinCompilerExtensionVersion
    // is no longer used — the plugin resolves the matching compiler automatically.

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt"
            )
        }
    }
}

// Modern Kotlin DSL — kotlinOptions {} is deprecated and will be removed in Gradle 10.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

/*
 * To use your own release keystore, run this once:
 *   keytool -genkey -v -keystore keystore/release.keystore \
 *           -alias autoclicker -keyalg RSA -keysize 2048 -validity 10000
 * Then put credentials in keystore/release.properties (NOT committed):
 *   RELEASE_STORE_FILE=../keystore/release.keystore
 *   RELEASE_STORE_PASSWORD=your_password
 *   RELEASE_KEY_ALIAS=autoclicker
 *   RELEASE_KEY_PASSWORD=your_key_password
 * And uncomment:
 *
 * signingConfigs {
 *     create("release") {
 *         val propsFile = rootProject.file("keystore/release.properties")
 *         if (propsFile.exists()) {
 *             val props = java.util.Properties().apply { propsFile.inputStream().use { load(it) } }
 *             storeFile = file(props.getProperty("RELEASE_STORE_FILE", "release.keystore"))
 *             storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
 *             keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
 *             keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
 *         }
 *     }
 * }
 */

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    // Required for ComposeView used outside an Activity (e.g. in a floating window):
    // we need a SavedStateRegistryOwner wired into the view tree, otherwise Compose 1.7+
    // silently refuses to compose and the window appears empty.
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
