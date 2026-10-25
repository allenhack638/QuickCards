package com.quickcards.app.core.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Locks the approved Stub Lock geometry. Every VectorDrawable that carries
 * the mark must use `docs/brand/stub-lock/pathdata.txt` verbatim, even-odd fill,
 * the Compact weight (0.9 scale around 54,54) and the specified layer colours.
 */
class BrandAssetsTest {

    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test
    fun foregroundMatchesTheLockedGeometry() {
        val vector = vector("ic_launcher_foreground.xml")
        assertVector(vector, widthDp = "108", heightDp = "108", viewport = "108")

        val group = singleGroup(vector)
        assertCompactWeight(group)

        val path = singlePath(vector)
        assertEquals("fill type", "evenOdd", path.android("fillType"))
        assertEquals("fill colour", "#FFFFFF", path.android("fillColor"))
        assertEquals("path data", expectedPathData(), path.android("pathData"))
    }

    @Test
    fun monochromeMatchesTheLockedGeometry() {
        val vector = vector("ic_launcher_monochrome.xml")
        assertVector(vector, widthDp = "108", heightDp = "108", viewport = "108")

        assertCompactWeight(singleGroup(vector))

        val path = singlePath(vector)
        assertEquals("fill type", "evenOdd", path.android("fillType"))
        assertEquals("fill colour", "#FF000000", path.android("fillColor"))
        assertEquals("path data", expectedPathData(), path.android("pathData"))
    }

    @Test
    fun notificationKeepsTheLockedGeometryInItsSixtyUnitBox() {
        val vector = vector("ic_stat_quickcards.xml")
        assertVector(vector, widthDp = "24", heightDp = "24", viewport = "60")

        val translate = directChildren(vector, "group").single()
        assertEquals("translateX", "-24", translate.android("translateX"))
        assertEquals("translateY", "-25", translate.android("translateY"))

        assertCompactWeight(directChildren(translate, "group").single())

        val path = singlePath(vector)
        assertEquals("fill type", "evenOdd", path.android("fillType"))
        assertEquals("fill colour", "#FFFFFF", path.android("fillColor"))
        assertEquals("path data", expectedPathData(), path.android("pathData"))
    }

    @Test
    fun noLayerKeepsTheRetiredClipPath() {
        listOf(
            "ic_launcher_foreground.xml",
            "ic_launcher_monochrome.xml",
            "ic_stat_quickcards.xml",
            "qc_app_mark.xml"
        ).forEach { name ->
            val vector = vector(name)
            assertTrue(
                "$name still contains a clip-path",
                allElements(vector, "clip-path").isEmpty()
            )
        }
    }

    private fun assertVector(vector: Element, widthDp: String, heightDp: String, viewport: String) {
        assertEquals("element", "vector", vector.tagName)
        assertEquals("width", "${widthDp}dp", vector.android("width"))
        assertEquals("height", "${heightDp}dp", vector.android("height"))
        assertEquals("viewportWidth", viewport, vector.android("viewportWidth"))
        assertEquals("viewportHeight", viewport, vector.android("viewportHeight"))
    }

    private fun assertCompactWeight(group: Element) {
        assertEquals("pivotX", "54", group.android("pivotX"))
        assertEquals("pivotY", "54", group.android("pivotY"))
        assertEquals("scaleX", "0.9", group.android("scaleX"))
        assertEquals("scaleY", "0.9", group.android("scaleY"))
    }

    private fun singleGroup(vector: Element): Element =
        directChildren(vector, "group").single()

    private fun singlePath(vector: Element): Element =
        vector.getElementsByTagName("path").let { nodes ->
            assertEquals("path count", 1, nodes.length)
            nodes.item(0) as Element
        }

    private fun expectedPathData(): String =
        File(repositoryRoot(), "docs/brand/stub-lock/pathdata.txt").readText().trim()

    private fun vector(name: String): Element {
        val file = File(repositoryRoot(), "app/src/main/res/drawable/$name")
        assertTrue("$name is missing", file.isFile)
        return document(file).documentElement
    }

    private fun document(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file)
    }

    private fun directChildren(parent: Element, tag: String): List<Element> =
        (0 until parent.childNodes.length)
            .mapNotNull { parent.childNodes.item(it) as? Element }
            .filter { it.tagName == tag }

    private fun allElements(parent: Element, tag: String): List<Element> {
        val nodes = parent.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.android(attribute: String): String =
        getAttributeNS(androidNamespace, attribute)

    private fun repositoryRoot(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (directory != null) {
            if (File(directory, "settings.gradle").isFile) return directory
            directory = directory.parentFile
        }
        throw AssertionError("Could not find settings.gradle from ${System.getProperty("user.dir")}")
    }
}
