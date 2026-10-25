package com.quickcards.app.core.ui

import androidx.compose.ui.unit.dp
import com.quickcards.app.core.ui.components.networkLeadWidth
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Manage lead column is the logo width unless a custom text badge needs more. */
class NetworkLeadWidthTest {

    @Test
    fun builtInMarksUseTheLogoWidth() {
        assertEquals(56.dp, networkLeadWidth(listOf(56.dp, 56.dp), 56.dp, 96.dp))
    }

    @Test
    fun aWiderCustomBadgeWidensTheColumn() {
        assertEquals(80.dp, networkLeadWidth(listOf(56.dp, 80.dp), 56.dp, 96.dp))
    }

    @Test
    fun theColumnStopsAtTheSlot() {
        assertEquals(96.dp, networkLeadWidth(listOf(56.dp, 140.dp), 56.dp, 96.dp))
    }

    @Test
    fun anEmptyListUsesTheMinimum() {
        assertEquals(56.dp, networkLeadWidth(emptyList(), 56.dp, 96.dp))
    }
}
