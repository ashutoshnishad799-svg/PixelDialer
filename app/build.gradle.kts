plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Sanity-check google-services.json BEFORE the google-services plugin's own
// processDebugGoogleServices task runs. That plugin task fails silently in
// ways that are easy to miss in CI logs (or, on older plugin versions, just
// skips writing default_web_client_id) when package_name in the JSON doesn't
// match applicationId below — which is exactly the mismatch that causes
// FirebaseAuth.getInstance()/FirebaseFirestore.getInstance() to throw
// IllegalStateException at runtime, i.e. the Home-screen-after-permissions
// crash. This task prints an impossible-to-miss log line so a CI run tells
// you immediately which one it is, instead of finding out from a crash on
// a physical device.
tasks.register("checkGoogleServicesPackageName") {
    doLast {
        val jsonFile = file("google-services.json")
        val expectedPackage = "com.pixeldialer.app"

        if (!jsonFile.exists()) {
            logger.error("")
            logger.error("========================================================================")
            logger.error("  ❌ app/google-services.json NOT FOUND")
            logger.error("  Firebase sign-in/cloud-backup will be silently disabled at runtime")
            logger.error("  (the app itself is coded to fail soft — it will NOT crash — but the")
            logger.error("  Account tab's sign-in button will show 'Sign-in isn't set up yet').")
            logger.error("========================================================================")
            logger.error("")
            return@doLast
        }

        val text = jsonFile.readText()
        // Cheap extraction, no JSON dep needed here: find "package_name": "..."
        val match = Regex("\"package_name\"\\s*:\\s*\"([^\"]+)\"").find(text)
        val foundPackages = Regex("\"package_name\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(text).map { it.groupValues[1] }.toSet()

        if (match == null) {
            logger.error("")
            logger.error("========================================================================")
            logger.error("  ❌ google-services.json found but no package_name field could be read")
            logger.error("  This usually means the file is corrupted, truncated, or not valid JSON.")
            logger.error("  Bytes: ${text.length}")
            logger.error("========================================================================")
            logger.error("")
            return@doLast
        }

        if (expectedPackage !in foundPackages) {
            logger.error("")
            logger.error("========================================================================")
            logger.error("  ❌ PACKAGE NAME MISMATCH — this is almost certainly your crash cause")
            logger.error("")
            logger.error("  app/build.gradle.kts applicationId : $expectedPackage")
            logger.error("  google-services.json package_name  : ${foundPackages.joinToString(", ")}")
            logger.error("")
            logger.error("  Fix: in Firebase Console → Project Settings → Your apps → Android app,")
            logger.error("  the Android package name MUST be exactly '$expectedPackage'.")
            logger.error("  If it isn't, add a NEW Android app in Firebase Console with that exact")
            logger.error("  package name, download ITS google-services.json, base64 it, and replace")
            logger.error("  the GOOGLE_SERVICES_JSON GitHub secret with the new value.")
            logger.error("========================================================================")
            logger.error("")
        } else {
            logger.lifecycle("✅ google-services.json package_name matches applicationId ($expectedPackage)")
        }
    }
}

tasks.matching { it.name == "processDebugGoogleServices" || it.name == "processReleaseGoogleServices" }
    .configureEach {
        dependsOn("checkGoogleServicesPackageName")
    }

android {
    namespace = "com.pixeldialer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixeldialer.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Accompanist (permissions)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Room (call log cache / favourites / blocked numbers)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // DataStore (theme preference: gradient vs solid)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (contact photo loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase (Auth for sign-in, Firestore for cloud backup/sync)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
