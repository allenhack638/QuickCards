package com.quickcards.app.core.ui

import com.quickcards.app.core.ui.components.MarkSurface
import com.quickcards.app.core.ui.components.networkMarkPlate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only the Manage list drops the white plate, and only in the light theme. */
class NetworkMarkPlateTest {

    @Test
    fun manageListInLightHasNoPlate() {
        assertFalse(networkMarkPlate(MarkSurface.LIST, dark = false))
    }

    @Test
    fun manageListInDarkKeepsThePlate() {
        assertTrue(networkMarkPlate(MarkSurface.LIST, dark = true))
    }

    @Test
    fun bandKeepsThePlateInBothThemes() {
        assertTrue(networkMarkPlate(MarkSurface.BAND, dark = false))
        assertTrue(networkMarkPlate(MarkSurface.BAND, dark = true))
    }

    @Test
    fun fieldKeepsThePlateInBothThemes() {
        assertTrue(networkMarkPlate(MarkSurface.FIELD, dark = false))
        assertTrue(networkMarkPlate(MarkSurface.FIELD, dark = true))
    }
}
