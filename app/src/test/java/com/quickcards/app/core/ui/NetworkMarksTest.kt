package com.quickcards.app.core.ui

import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.ui.components.NetworkMarks
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every built-in network has a mark, and every mark sits on the same 56x28 badge canvas. */
class NetworkMarksTest {

    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test
    fun everyBuiltInNetworkHasAMark() {
        val builtIn = SeedData.networks.map { it.id }.toSet()
        assertEquals(builtIn, NetworkMarks.ids)
    }

    @Test
    fun everyMarkUsesTheStandardCanvas() {
        for (id in NetworkMarks.ids) {
            val file = File("src/main/res/drawable/net_$id.xml")
            assertTrue("$id drawable missing", file.isFile)
            val vector = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder().parse(file).documentElement
            assertEquals("$id width", "56dp", vector.getAttributeNS(androidNamespace, "width"))
            assertEquals("$id height", "28dp", vector.getAttributeNS(androidNamespace, "height"))
            assertEquals("$id viewport", "56", vector.getAttributeNS(androidNamespace, "viewportWidth"))
            assertEquals("$id viewport", "28", vector.getAttributeNS(androidNamespace, "viewportHeight"))
            val group = vector.getElementsByTagName("group").item(0)
            assertTrue("$id has no fitting group", group != null)
            assertTrue("$id over 96 KB", file.length() < 96 * 1024)
        }
    }
}
