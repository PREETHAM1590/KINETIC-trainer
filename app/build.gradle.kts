import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile
val googleServicesTemplateFile = layout.projectDirectory.file("google-services.json.template").asFile
val injectedGoogleServicesPath = providers.environmentVariable("GOOGLE_SERVICES_JSON_PATH")

fun isReleaseBuildRequested(taskNames: List<String>): Boolean {
    if (taskNames.isEmpty()) return false
    return taskNames.any { task ->
        val normalized = task.lowercase()
        (normalized.contains("release") || normalized.contains("bundle") || normalized.contains("publish")) &&
            !normalized.contains("debug")
    }
}

tasks.register("prepareGoogleServicesConfig") {
    group = "verification"
    description = "Ensures google-services.json is present via injection or template fallback."

    doLast {
        val releaseRequested = isReleaseBuildRequested(gradle.startParameter.taskNames)
        val injectedPath = injectedGoogleServicesPath.orNull?.trim().orEmpty()

        if (injectedPath.isNotEmpty()) {
            val source = file(injectedPath)
            if (!source.exists()) {
                throw GradleException("GOOGLE_SERVICES_JSON_PATH points to missing file: $injectedPath")
            }
            source.copyTo(googleServicesFile, overwrite = true)
        } else if (!googleServicesFile.exists()) {
            if (releaseRequested) {
                throw GradleException(
                    "Missing app/google-services.json for release build. " +
                        "Set GOOGLE_SERVICES_JSON_PATH to a real config file or add app/google-services.json locally."
                )
            }
            if (!googleServicesTemplateFile.exists()) {
                throw GradleException(
                    "Missing app/google-services.json and template fallback app/google-services.json.template."
                )
            }
            googleServicesTemplateFile.copyTo(googleServicesFile, overwrite = true)
        }

        if (releaseRequested) {
            val content = googleServicesFile.readText()
            if (content.contains("template-project-id") || content.contains("template-mobilesdk-app-id")) {
                throw GradleException(
                    "Release build requires real Firebase config. Template markers detected in app/google-services.json."
                )
            }
        }
    }
}

tasks.matching {
    it.name == "preBuild" || it.name == "preDebugBuild" || it.name == "preReleaseBuild"
}.configureEach {
    dependsOn("prepareGoogleServicesConfig")
}


android {
    namespace = "com.kinetic.trainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kinetic.trainer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    implementation("org.whispersystems:signal-protocol-android:2.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
