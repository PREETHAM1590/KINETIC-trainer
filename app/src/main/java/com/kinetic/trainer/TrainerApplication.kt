package com.kinetic.trainer

import android.app.Application
import android.content.pm.ApplicationInfo
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
		val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
		FirebaseApp.initializeApp(this)
		FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
			appCheckProviderFactoryForBuild(isDebugBuild)
		)
	}
}
