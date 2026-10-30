package com.quickcards.domain

/** When the Google Play update prompt appears: once per new Play version, never repeated. */
object UpdatePolicy {

    fun shouldPrompt(
        installedVersion: Int,
        availableVersion: Int,
        lastPromptedVersion: Int,
        flexibleAllowed: Boolean
    ): Boolean =
        flexibleAllowed &&
            availableVersion > installedVersion &&
            availableVersion != lastPromptedVersion
}
