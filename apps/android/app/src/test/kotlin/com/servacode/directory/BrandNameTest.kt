package com.servacode.directory

import com.servacode.directory.core.model.DirectoryBrand
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app's name exists twice, and only twice, and the two must agree.
 *
 * The manifest cannot read a Kotlin constant, so the launcher label is a string resource while
 * everything the app draws reads `DirectoryBrand.NAME`. Before they were joined the name was
 * written as a literal in five places, in two different spellings, and none of them was the
 * name. This test is what stops that happening again.
 */
class BrandNameTest {
    private fun file(relative: String): File =
        listOf(relative, "app/$relative", "../app/$relative")
            .map(::File)
            .first(File::isFile)

    private fun stringResource(name: String): String {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file("src/main/res/values/strings.xml"))
        val strings = document.getElementsByTagName("string")
        for (index in 0 until strings.length) {
            val node = strings.item(index)
            if (node.attributes.getNamedItem("name").nodeValue == name) return node.textContent
        }
        throw AssertionError("no string resource named $name")
    }

    @Test
    fun `the launcher label is the app's own name`() {
        assertEquals(DirectoryBrand.NAME, stringResource("app_name"))
    }

    @Test
    fun `the manifest takes its label from the resource, not from a literal`() {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        assertEquals(
            "the manifest should name the resource",
            true,
            """android:label="@string/app_name"""" in manifest,
        )
    }

    @Test
    fun `no screen writes the name as a literal of its own`() {
        val roots = listOf(File("../feature"), File("feature"), File("../core"), File("core"))
            .filter(File::isDirectory)
        val offenders = roots
            .flatMap { it.walkTopDown().filter { file -> file.extension == "kt" } }
            .filterNot { "build" in it.path.split(File.separatorChar) }
            // The one place the name is allowed to be a literal is the constant itself.
            .filterNot { it.name == "DirectoryBrand.kt" }
            .filter { DirectoryBrand.NAME in it.readText() }
            .map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }
}
