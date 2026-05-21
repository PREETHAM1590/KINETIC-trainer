package com.kinetic.trainer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

internal fun appCheckProviderFactoryForBuild(isDebugBuild: Boolean): AppCheckProviderFactory {
return if (isDebugBuild) {
DebugAppCheckProviderFactory.getInstance()
} else {
PlayIntegrityAppCheckProviderFactory.getInstance()
}
}

@HiltAndroidApp
class TrainerApplication : Application() {

override fun onCreate() {
super.onCreate()

val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

// Initialize Timber logging
if (isDebugBuild) {
Timber.plant(Timber.DebugTree())
} else {
Timber.plant(ReleaseTree())
}

if (isUnitTestRuntime()) {
return
}

val firebaseApp = runCatching {
FirebaseApp.initializeApp(this)
}.getOrElse { error ->
if (isDebugBuild) {
Timber.w(error, "Firebase initialization skipped in debug/test runtime")
return
}
throw IllegalStateException("Firebase initialization failed in non-debug build", error)
}

if (firebaseApp == null) {
if (isDebugBuild) {
Timber.w("Firebase initialization skipped because options are missing in debug/test runtime")
return
}
throw IllegalStateException("Firebase initialization failed in non-debug build")
}

FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
appCheckProviderFactoryForBuild(isDebugBuild)
)
}

companion object {
private const val TAG = "TrainerApplication"
}

private fun isUnitTestRuntime(): Boolean {
return System.getProperty("java.class.path")?.contains("robolectric", ignoreCase = true) == true
}

/**
 * Production logging tree that filters out verbose/debug logs
 * and reports errors to Crashlytics
 */
private class ReleaseTree : Timber.Tree() {
override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
return
}

// In production, we'd report errors to Crashlytics here
// For now just use standard Android logging filtered by priority
if (priority >= android.util.Log.INFO) {
if (t != null) {
android.util.Log.println(priority, tag ?: "KineticTrainer", "$message\n${android.util.Log.getStackTraceString(t)}")
} else {
android.util.Log.println(priority, tag ?: "KineticTrainer", message)
}
}
}
}
}