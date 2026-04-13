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
		if (isUnitTestRuntime()) {
			return
		}

		val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
		val firebaseApp = runCatching {
			FirebaseApp.initializeApp(this)
		}.getOrElse { error ->
			if (isDebugBuild) {
				Log.w(TAG, "Firebase initialization skipped in debug/test runtime", error)
				return
			}
			throw IllegalStateException("Firebase initialization failed in non-debug build", error)
		}

		if (firebaseApp == null) {
			if (isDebugBuild) {
				Log.w(TAG, "Firebase initialization skipped because options are missing in debug/test runtime")
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
}
