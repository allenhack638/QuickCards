package com.quickcards.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the merged debug and release manifests: any network or camera permission or
 * background upload scheduler fails.
 */
class OfflineManifestGuardTest {

    private val forbidden = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.CAMERA",
        "com.google.android.datatransport"
    )

    @Test
    fun mergedManifestsStayOffline() {
        val manifests = listOf(
            "debug" to System.getProperty("qc.mergedDebugManifest"),
            "release" to System.getProperty("qc.mergedReleaseManifest")
        )

        manifests.forEach { (variant, path) ->
            assertTrue("No merged $variant manifest path was provided", !path.isNullOrBlank())
            val manifest = File(path!!)
            assertTrue("Merged $variant manifest is missing: $manifest", manifest.isFile())
            val text = manifest.readText()
            forbidden.forEach { token ->
                assertFalse(
                    "Merged $variant manifest contains $token",
                    text.contains(token)
                )
            }
        }
    }
}
