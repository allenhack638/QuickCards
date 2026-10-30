package com.quickcards.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    @Test
    fun `a newer Play version prompts once`() {
        assertTrue(UpdatePolicy.shouldPrompt(10, 11, lastPromptedVersion = 0, flexibleAllowed = true))
        assertFalse(UpdatePolicy.shouldPrompt(10, 11, lastPromptedVersion = 11, flexibleAllowed = true))
    }

    @Test
    fun `a later version prompts again after an earlier one was declined`() {
        assertTrue(UpdatePolicy.shouldPrompt(10, 12, lastPromptedVersion = 11, flexibleAllowed = true))
    }

    @Test
    fun `no prompt without a newer version or when Play does not allow it`() {
        assertFalse(UpdatePolicy.shouldPrompt(11, 11, lastPromptedVersion = 0, flexibleAllowed = true))
        assertFalse(UpdatePolicy.shouldPrompt(10, 11, lastPromptedVersion = 0, flexibleAllowed = false))
    }
}
