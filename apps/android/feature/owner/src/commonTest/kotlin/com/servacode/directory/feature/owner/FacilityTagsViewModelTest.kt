package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** The connection as a test sets it; the monitor itself needs the Android framework. */
private class FakeNetwork(online: Boolean = true) : NetworkMonitor {
    val state = MutableStateFlow(online)
    override val online: Flow<Boolean> = state
    override val unmetered: Flow<Boolean> = state
}

class FacilityTagsViewModelTest {
    private val api = ScriptedOwnerApi()
    private val network = FakeNetwork()
    private val sent = mutableListOf<OwnerFacilityPatch>()

    private fun viewModel(): FacilityTagsViewModel {
        val repository = OwnerRepository(api)
        return FacilityTagsViewModel(LoadTagChoicesUseCase(repository), ManageFacilityUseCase(repository), network)
    }

    /** The backend's answer to a patch: the facility carrying what was sent. */
    private fun acceptPatches(start: OwnerFacilityDetail) {
        var facility = start
        api.patchAnswer = { _, patch ->
            sent += patch
            facility = facility.copy(
                specialtyIds = patch.specialtyIds ?: facility.specialtyIds,
                serviceTagIds = patch.serviceTagIds ?: facility.serviceTagIds,
            )
            facility
        }
    }

    private fun FacilityTagsViewModel.content() = state.value as FacilityTagsUiState.Content

    @Test fun `loading and then the category's choices with what the facility carries ticked`() =
        runMainTest {
            api.configAnswer = { ownerConfig(clinicTags) }
            val model = viewModel()

            model.show(tagged(specialties = listOf("3"), services = listOf("12")))
            assertEquals(FacilityTagsUiState.Loading, model.state.value)
            advanceUntilIdle()

            val content = model.content()
            assertEquals(clinicTags, content.form.choices)
            assertEquals(setOf("3"), content.form.specialties)
            assertEquals(setOf("12"), content.form.services)
            assertFalse(content.canSave)
            assertEquals(listOf("config:raqqa"), api.calls)
        }

    @Test fun `a category with nothing to pick or not open to owners here draws nothing`() =
        runMainTest {
            api.configAnswer = { ownerConfig(CategoryTags()) }
            val empty = viewModel()
            empty.show(tagged())
            advanceUntilIdle()

            api.configAnswer = { ownerConfig(clinicTags, categoryId = "another") }
            val unlisted = viewModel()
            unlisted.show(tagged())
            advanceUntilIdle()

            assertEquals(FacilityTagsUiState.Hidden, empty.state.value)
            assertEquals(FacilityTagsUiState.Hidden, unlisted.state.value)
        }

    @Test fun `one group without choices is left out and not the section`() = runMainTest {
        api.configAnswer = { ownerConfig(clinicTags.copy(specialties = emptyList())) }
        val model = viewModel()

        model.show(tagged(services = listOf("12")))
        advanceUntilIdle()

        assertTrue(model.content().form.choices.specialties.isEmpty())
        assertEquals(setOf("12"), model.content().form.services)
    }

    @Test fun `a failure to read the choices is shown and retry asks again`() = runMainTest {
        api.configAnswer = { throw AppException(AppError(AppError.Kind.SERVER, status = 500)) }
        val model = viewModel()
        model.show(tagged())
        advanceUntilIdle()
        assertEquals(AppError.Kind.SERVER, (model.state.value as FacilityTagsUiState.Error).error.kind)

        api.configAnswer = { ownerConfig(clinicTags) }
        model.retry()
        advanceUntilIdle()

        assertTrue(model.state.value is FacilityTagsUiState.Content)
        assertEquals(listOf("config:raqqa", "config:raqqa"), api.calls)
    }

    @Test fun `ticking and saving sends only the list that changed`() = runMainTest {
        val facility = tagged(specialties = listOf("3"), services = listOf("12"))
        api.configAnswer = { ownerConfig(clinicTags) }
        acceptPatches(facility)
        val model = viewModel()
        model.show(facility)
        advanceUntilIdle()

        model.toggleSpecialty("4")
        model.toggleSpecialty("3")
        assertTrue(model.content().canSave)
        model.save()
        assertTrue(model.content().saving)
        assertFalse(model.content().editable)
        advanceUntilIdle()

        assertEquals(listOf(OwnerFacilityPatch(specialtyIds = listOf("4"))), sent)
        val saved = model.content()
        assertTrue(saved.saved)
        assertFalse(saved.saving)
        // What the backend answered is now what is saved: nothing is left to send.
        assertEquals(setOf("4"), saved.form.savedSpecialties)
        assertFalse(saved.form.changed)
        assertTrue(api.calls.contains("patch:f-1"))
    }

    @Test fun `nothing changed and nothing is sent`() = runMainTest {
        api.configAnswer = { ownerConfig(clinicTags) }
        acceptPatches(tagged())
        val model = viewModel()
        model.show(tagged())
        advanceUntilIdle()

        model.toggleService("13")
        model.toggleService("13")
        model.save()
        advanceUntilIdle()

        assertTrue(sent.isEmpty())
    }

    @Test fun `a refused choice says the list changed and a fresh list keeps the ticks still offered`() =
        runMainTest {
            api.configAnswer = { ownerConfig(clinicTags) }
            api.patchAnswer = { _, _ ->
                throw AppException(
                    AppError(
                        AppError.Kind.VALIDATION,
                        code = "VALIDATION_ERROR",
                        fieldErrors = mapOf("specialtyIds" to listOf("One or more specialties are invalid.")),
                        status = 400,
                    ),
                )
            }
            val model = viewModel()
            model.show(tagged(services = listOf("12")))
            advanceUntilIdle()
            model.toggleSpecialty("4")
            model.toggleSpecialty("5")

            model.save()
            advanceUntilIdle()
            assertEquals(FacilityTagsFailure.ChoicesOutdated, model.content().failure)

            // «أطفال» was retired meanwhile.
            api.configAnswer = {
                ownerConfig(clinicTags.copy(specialties = clinicTags.specialties.filter { it.id != "4" }))
            }
            model.retry()
            advanceUntilIdle()

            val refreshed = model.content()
            assertNull(refreshed.failure)
            assertEquals(setOf("5"), refreshed.form.specialties)
            assertTrue(refreshed.canSave)
        }

    @Test fun `any other refusal keeps the ticks and the error's own reason`() = runMainTest {
        api.configAnswer = { ownerConfig(clinicTags) }
        val model = viewModel()
        model.show(tagged())
        advanceUntilIdle()
        model.toggleService("12")

        model.save()
        advanceUntilIdle()

        val failed = model.content()
        assertEquals(AppError.Kind.OFFLINE, (failed.failure as FacilityTagsFailure.Failed).error.kind)
        assertEquals(setOf("12"), failed.form.services)
        assertTrue(failed.canSave)
        // The next tick clears what was said about the last attempt.
        model.toggleService("13")
        assertNull(model.content().failure)
    }

    @Test fun `offline the choices stay in sight and cannot change`() = runMainTest {
        api.configAnswer = { ownerConfig(clinicTags) }
        acceptPatches(tagged())
        val model = viewModel()
        model.show(tagged(specialties = listOf("3")))
        advanceUntilIdle()
        model.toggleService("12")

        network.state.value = false
        advanceUntilIdle()
        model.toggleSpecialty("4")
        model.save()
        advanceUntilIdle()

        val offline = model.content()
        assertTrue(offline.offline)
        assertFalse(offline.editable)
        assertFalse(offline.canSave)
        assertEquals(setOf("3"), offline.form.specialties)
        assertEquals(setOf("12"), offline.form.services)
        assertTrue(sent.isEmpty())

        network.state.value = true
        advanceUntilIdle()
        assertTrue(model.content().canSave)
    }

    @Test fun `choices that could not be read offline are read again when the connection returns`() =
        runMainTest {
            val model = viewModel()
            model.show(tagged())
            advanceUntilIdle()
            assertEquals(AppError.Kind.OFFLINE, (model.state.value as FacilityTagsUiState.Error).error.kind)

            api.configAnswer = { ownerConfig(clinicTags) }
            network.state.value = false
            advanceUntilIdle()
            network.state.value = true
            advanceUntilIdle()

            assertTrue(model.state.value is FacilityTagsUiState.Content)
        }

    @Test fun `the screen reading the facility again keeps unsaved ticks and asks for nothing`() =
        runMainTest {
            api.configAnswer = { ownerConfig(clinicTags) }
            val model = viewModel()
            model.show(tagged(specialties = listOf("3")))
            advanceUntilIdle()
            model.toggleService("13")

            model.show(tagged(specialties = listOf("5")))
            advanceUntilIdle()

            val form = model.content().form
            assertEquals(setOf("5"), form.specialties)
            assertEquals(setOf("13"), form.services)
            assertEquals(listOf("config:raqqa"), api.calls)
        }

    @Test fun `an id the category does not offer never reaches the backend`() = runMainTest {
        api.configAnswer = { ownerConfig(clinicTags.copy(services = listOf(FacilityTag("12", "قياس ضغط")))) }
        acceptPatches(tagged())
        val model = viewModel()
        model.show(tagged())
        advanceUntilIdle()

        model.toggleService("99")
        model.toggleService("12")
        model.save()
        advanceUntilIdle()

        assertEquals(listOf(OwnerFacilityPatch(serviceTagIds = listOf("12"))), sent)
    }
}
