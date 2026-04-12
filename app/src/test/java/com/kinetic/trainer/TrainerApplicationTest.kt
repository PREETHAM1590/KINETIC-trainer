package com.kinetic.trainer

import com.google.common.truth.Truth.assertThat
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.junit.Test

class TrainerApplicationTest {

    @Test
    fun debugBuild_usesDebugAppCheckProvider() {
        val provider = appCheckProviderFactoryForBuild(isDebugBuild = true)

        assertThat(provider).isInstanceOf(DebugAppCheckProviderFactory::class.java)
    }

    @Test
    fun releaseBuild_usesPlayIntegrityAppCheckProvider() {
        val provider = appCheckProviderFactoryForBuild(isDebugBuild = false)

        assertThat(provider).isInstanceOf(PlayIntegrityAppCheckProviderFactory::class.java)
    }
}
