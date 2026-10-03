package com.servacode.directory.feature.settings

import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.advanceUntilIdle

class LegalViewModelTest {
    private val about = LegalPage(LegalPageKey.ABOUT, "من نحن", version = 2, bodyAr = "دليل الأماكن في سوريا")

    @Test fun `the published pages are listed`() = runMainTest {
        val api = ScriptedPublicApi().apply { legalPagesAnswer = { listOf(about) } }
        val viewModel = LegalViewModel(LegalRepository(api))
        advanceUntilIdle()

        assertEquals(LegalListState.Content(listOf(about)), viewModel.pages.value)
    }

    @Test fun `a page read once still opens when the connection has gone`() = runMainTest {
        var online = true
        val api = ScriptedPublicApi().apply {
            legalPagesAnswer = { emptyList() }
            legalPageAnswer = { if (online) about else error("offline") }
        }
        val repository = LegalRepository(api)
        LegalViewModel(repository).apply { open(LegalPageKey.ABOUT) }
        advanceUntilIdle()

        online = false
        val again = LegalViewModel(repository)
        again.open(LegalPageKey.ABOUT)
        advanceUntilIdle()

        assertEquals(LegalPageState.Content(about), again.page.value)
    }

    @Test fun `a page never read and not reachable is an error to retry`() = runMainTest {
        val viewModel = LegalViewModel(LegalRepository(ScriptedPublicApi()))
        viewModel.open(LegalPageKey.TERMS)
        advanceUntilIdle()

        assertIs<LegalPageState.Error>(viewModel.page.value)
        assertIs<LegalListState.Error>(viewModel.pages.value)
    }
}
