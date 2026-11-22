package com.quickcards.app.core.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QcWindowTest {

    @Test
    fun phonePortraitIsCompactWithTheBottomBar() {
        val window = QcWindow.of(412.dp, 915.dp)
        assertEquals(WidthClass.COMPACT, window.width)
        assertFalse(window.rail)
        assertFalse(window.detailBesideList)
        assertFalse(window.formDialog)
        assertFalse(window.sideBySideDetails)
    }

    @Test
    fun phoneLandscapeKeepsFullScreenDetailsAndForm() {
        val window = QcWindow.of(780.dp, 360.dp)
        assertEquals(WidthClass.MEDIUM, window.width)
        assertTrue(window.compactHeight)
        assertTrue(window.rail)
        assertTrue(window.sideBySideDetails)
        assertFalse(window.detailBesideList)
        assertFalse(window.formDialog)
    }

    @Test
    fun wideLandscapePhoneStaysShortHeight() {
        val window = QcWindow.of(915.dp, 412.dp)
        assertEquals(WidthClass.EXPANDED, window.width)
        assertFalse(window.twoPane)
        assertFalse(window.settingsTwoPane)
        assertTrue(window.sideBySideDetails)
    }

    @Test
    fun mediumOpensTheCardBesideTheList() {
        val window = QcWindow.of(700.dp, 840.dp)
        assertEquals(WidthClass.MEDIUM, window.width)
        assertFalse(window.twoPane)
        assertTrue(window.detailBesideList)
        assertTrue(window.formDialog)
        assertFalse(window.settingsTwoPane)
    }

    @Test
    fun expandedIsAlwaysListDetail() {
        val window = QcWindow.of(1280.dp, 800.dp)
        assertEquals(WidthClass.EXPANDED, window.width)
        assertTrue(window.twoPane)
        assertTrue(window.settingsTwoPane)
    }

    @Test
    fun breakpointsAreInclusive() {
        assertEquals(WidthClass.MEDIUM, QcWindow.of(600.dp, 900.dp).width)
        assertEquals(WidthClass.EXPANDED, QcWindow.of(840.dp, 900.dp).width)
        assertFalse(QcWindow.of(700.dp, 480.dp).compactHeight)
    }

    @Test
    fun aSeparatingHingeForcesTwoPanesAtMediumWidth() {
        val window = QcWindow.of(673.dp, 841.dp, Hinge(left = 1000, right = 1000))
        assertEquals(WidthClass.MEDIUM, window.width)
        assertTrue(window.twoPane)
    }
}
