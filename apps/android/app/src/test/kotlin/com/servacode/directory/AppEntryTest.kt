package com.servacode.directory

import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.DeepLinkTarget
import com.servacode.directory.core.model.NotificationCategory
import com.servacode.directory.core.model.NotificationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppEntryTest {
    private val hosts = setOf("daliini.example")
    private val id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    private val view = "android.intent.action.VIEW"

    private fun entry(action: String?, data: String?, extras: Map<String, String> = emptyMap()) =
        AppEntries.from(action, data, hosts) { extras[it] }

    @Test fun `an App Link opens its place`() {
        assertEquals(AppEntry.Link(DeepLinkTarget.Facility(id)), entry(view, "https://daliini.example/f/$id"))
        assertEquals(AppEntry.Link(DeepLinkTarget.DutyNow), entry(view, "https://www.daliini.example/duty"))
        assertNull(entry(view, "https://elsewhere.example/duty"))
        assertNull(entry(null, "https://daliini.example/duty"))
    }

    @Test fun `a tapped notice opens where its type leads`() {
        val gap = entry(
            null,
            null,
            mapOf("com.servacode.directory.notice.TYPE" to "duty.gap_nudge", "com.servacode.directory.notice.DATE" to "2026-09-30"),
        ) as AppEntry.Notice
        // No facility comes with a nudge today: the owner picks one from their list.
        assertEquals(NotificationTarget.DutyScheduling(null, "2026-09-30"), gap.target)

        val broadcast = entry(null, null, mapOf("com.servacode.directory.notice.TYPE" to "platform.broadcast"))
        assertEquals(NotificationTarget.None, (broadcast as AppEntry.Notice).target)
    }

    @Test fun `what another app puts in the extras is checked`() {
        val crafted = entry(
            null,
            null,
            mapOf(
                "com.servacode.directory.notice.TYPE" to "facility.hours.confirm_request",
                "com.servacode.directory.notice.FACILITY_ID" to "../../etc",
            ),
        ) as AppEntry.Notice
        assertNull(crafted.facilityId)
        assertNull(entry(null, null, mapOf("com.servacode.directory.notice.TYPE" to "<script>")))
        assertNull(entry(null, null))
    }

    @Test fun `turned-off kinds are not shown, and nothing unknown is hidden`() {
        val quiet = NotificationPreferences(dutyReminders = false, provinceNews = false, applicationStatus = false)
        assertFalse(quiet.allows(NotificationCategory.DUTY_REMINDER))
        assertFalse(quiet.allows(NotificationCategory.PROVINCE_NEWS))
        assertFalse(quiet.allows(NotificationCategory.APPLICATION_STATUS))
        assertTrue(quiet.allows(null))
        assertTrue(NotificationPreferences().allows(NotificationCategory.of("duty.gap_nudge")))
    }

    @Test fun `every province the backend serves is claimed by the manifest, and nothing else`() {
        val reference = listOf(
            "../../backend/directory/reference_data/launch_v1.py",
            "../backend/directory/reference_data/launch_v1.py",
            "apps/backend/directory/reference_data/launch_v1.py",
        ).map(::File).firstOrNull(File::isFile)
        assumeTrue("the backend's reference data is not in this checkout", reference != null)
        val codes = Regex("""^\s*\("([a-z-]+)", "[^"]+", "[^"]+", (True|False), \d+\),""", RegexOption.MULTILINE)
            .findAll(reference!!.readText())
            .map { it.groupValues[1] }
            .toSet()

        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
            .map(::File).first(File::isFile)
        val data = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest).getElementsByTagName("data")
        val paths = (0 until data.length)
            .mapNotNull { data.item(it).attributes.getNamedItemNS(ANDROID, "path")?.nodeValue }
            .toSet()

        assertEquals(14, codes.size)
        assertEquals(codes.map { "/$it" }.toSet() + setOf("/duty", "/duty/today"), paths)
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
