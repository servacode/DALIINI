package com.servacode.directory

import com.servacode.directory.core.model.AppRelease
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Who is stopped, and — more importantly — who is not.
 *
 * The cases that matter here are the ones where the answer is missing or empty. A build that
 * keeps running for one more session because a request failed costs little; every phone locked
 * out of a working platform because a request failed costs everything.
 */
class VersionCheckTest {
    private fun check(answer: () -> AppRelease) = VersionCheck(
        ScriptedPublicApi().apply { appReleaseAnswer = answer },
    )

    @Test fun `a build below the minimum is stopped, and told where to go`() = runTest {
        val subject = check { AppRelease(12, 19, "https://play.example.test/app", "حدّث التطبيق.") }

        subject.refresh(versionCode = 11)

        val verdict = subject.verdict.value as VersionCheck.Verdict.TooOld
        assertEquals("حدّث التطبيق.", verdict.notice)
        assertEquals("https://play.example.test/app", verdict.storeUrl)
    }

    @Test fun `the minimum itself is allowed`() = runTest {
        val subject = check { AppRelease(12, 19, "", "") }
        subject.refresh(versionCode = 12)
        assertEquals(VersionCheck.Verdict.Allowed, subject.verdict.value)
    }

    @Test fun `an older build with nothing configured is not stopped`() = runTest {
        // What an unconfigured backend answers. Zero blocks nobody.
        val subject = check { AppRelease(0, 0, "", "") }
        subject.refresh(versionCode = 1)
        assertEquals(VersionCheck.Verdict.Allowed, subject.verdict.value)
    }

    @Test fun `a backend that cannot be reached blocks nobody`() = runTest {
        val subject = check { error("no network") }

        subject.refresh(versionCode = 1)

        // Not Allowed either: nothing was learned, and the gate shows nothing on Unknown.
        assertEquals(VersionCheck.Verdict.Unknown, subject.verdict.value)
    }

    @Test fun `an empty notice and an empty link become nothing, not empty strings`() = runTest {
        // The screen decides on null: a button with no address is not shown at all.
        val subject = check { AppRelease(5, 5, "", "") }

        subject.refresh(versionCode = 1)

        val verdict = subject.verdict.value as VersionCheck.Verdict.TooOld
        assertNull(verdict.notice)
        assertNull(verdict.storeUrl)
    }

    @Test fun `a working build with a newer one after it is offered the newer one`() = runTest {
        val subject = check { AppRelease(12, 19, "https://play.example.test/app", "") }

        subject.refresh(versionCode = 15)

        assertEquals(
            VersionCheck.Verdict.Newer(19, notice = null, storeUrl = "https://play.example.test/app"),
            subject.verdict.value,
        )
    }

    @Test fun `no store, no offer`() = runTest {
        // An offer the reader cannot act on is noise; the build simply runs.
        val subject = check { AppRelease(12, 19, "", "تحديث متاح") }
        subject.refresh(versionCode = 15)
        assertEquals(VersionCheck.Verdict.Allowed, subject.verdict.value)
    }

    @Test fun `the newest build is offered nothing`() = runTest {
        val subject = check { AppRelease(12, 19, "https://play.example.test/app", "") }
        subject.refresh(versionCode = 19)
        assertEquals(VersionCheck.Verdict.Allowed, subject.verdict.value)
    }

    @Test fun `the release itself knows what it blocks and what it supersedes`() {
        val release = AppRelease(minimumVersionCode = 10, latestVersionCode = 20, storeUrl = "", noticeAr = "")

        assertTrue(release.blocks(9))
        assertTrue(!release.blocks(10))
        // Between the two: usable, but not the newest.
        assertTrue(release.supersedes(15))
        assertTrue(!release.supersedes(20))
        // A blocked build is not merely superseded; it is refused.
        assertTrue(!release.supersedes(9))
    }
}
