plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    val xcfName = "ComposeApp"
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        if (konanTarget.family == org.jetbrains.konan.target.Family.IOS) {
            binaries.framework {
                baseName = xcfName
                isStatic = true
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        val androidMain by getting {
            kotlin.srcDir("src/main/java")
            dependencies {
                val hilt_version = "2.50"
                val room_version = "2.6.1"
                val work_version = "2.9.0"

                // Hilt
                implementation("com.google.dagger:hilt-android:$hilt_version")
                implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
                implementation("androidx.hilt:hilt-work:1.1.0")

                // Core
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
                implementation("androidx.navigation:navigation-compose:2.7.7")

                // Compose
                implementation(platform("androidx.compose:compose-bom:2024.02.00"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.runtime:runtime-livedata")

                // Room
                implementation("androidx.room:room-runtime:$room_version")
                implementation("androidx.room:room-ktx:$room_version")

                // WorkManager
                implementation("androidx.work:work-runtime-ktx:$work_version")

                // DataStore
                implementation("androidx.datastore:datastore-preferences:1.0.0")

                // Google Drive REST API & Auth
                implementation("com.google.android.gms:play-services-auth:21.0.0")
                implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
                implementation("com.google.api-client:google-api-client-android:2.2.0")
                implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")

                // JSON
                implementation("com.google.code.gson:gson:2.10.1")

                // Image Loading (GIF support)
                implementation("io.coil-kt:coil-compose:2.6.0")
                implementation("io.coil-kt:coil-gif:2.6.0")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.fitness"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitness"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "0.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/main/AndroidManifest.xml")
        java.srcDirs("src/main/java")
        res.srcDirs("src/main/res")
        assets.srcDirs("src/main/assets")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
        }
    }
}

dependencies {
    val room_version = "2.6.1"
    val hilt_version = "2.50"

    ksp("com.google.dagger:hilt-android-compiler:$hilt_version")
    ksp("androidx.hilt:hilt-compiler:1.1.0")
    ksp("androidx.room:room-compiler:$room_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
