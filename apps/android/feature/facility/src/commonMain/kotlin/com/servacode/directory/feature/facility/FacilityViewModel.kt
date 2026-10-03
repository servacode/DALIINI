package com.servacode.directory.feature.facility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.analytics.AnalyticsEvent
import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface FacilityUiState {
    data object Loading : FacilityUiState
    data class Content(
        val value: FacilityDetail,
        val stale: Boolean,
        val signedIn: Boolean = false,
        val myRating: Int? = null,
        val ratingFailure: AppError? = null,
    ) : FacilityUiState
    data class Error(val error: AppError) : FacilityUiState
}

/**
 * One facility's page, on both platforms: [id] is the facility the page was opened for. Android's
 * navigation asks Hilt for the subclass in androidMain, which reads it from the route
 * (DECISION-095).
 */
open class FacilityViewModel(
    private val id: String,
    private val facility: FacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
    private val session: SessionCoordinator,
    private val recordVisit: RecordVisitUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow<FacilityUiState>(FacilityUiState.Loading)
    val state: StateFlow<FacilityUiState> = _state.asStateFlow()
    private var loading: Job? = null
    private var visitRecorded = false

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (RealtimeInvalidation.refreshesFacility(event, id)) refresh()
            }
        }
    }

    /**
     * The dialer was opened — not that a call was made, which the app cannot know and does not
     * claim to. The number itself is never sent; only that this facility's was tapped.
     */
    fun phoneTapped() = analytics.track(AnalyticsEvent.PhoneTap(facilityId = id))

    /** The way there was asked for. Where the person is is never part of it. */
    fun directionsStarted() =
        analytics.track(AnalyticsEvent.DirectionsStart(facilityId = id, routingProvider = "valhalla"))

    private fun signedIn() = session.state.value == SessionState.SIGNED_IN

    /**
     * Save or unsave the facility on screen.
     *
     * The heart fills at once and goes back if the backend refuses: the state shown belongs to
     * the account, and the backend is the one that holds it.
     */
    fun toggleFavorite() {
        val current = _state.value as? FacilityUiState.Content ?: return
        if (!current.signedIn) return
        val saved = current.value.summary.isFavorite
        _state.value = current.copy(
            value = current.value.copy(
                summary = current.value.summary.copy(isFavorite = !saved),
            ),
        )
        viewModelScope.launch {
            val result = runCatching {
                if (saved) facility.unsave(current.value.summary.id)
                else facility.save(current.value.summary.id)
            }
            result.onFailure { _state.value = current }
        }
    }

    private fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            facility(id).collect { loaded ->
                val previous = _state.value as? FacilityUiState.Content
                _state.value = when (loaded) {
                    is Loaded.Cached -> content(loaded.value, stale = false, previous)
                    is Loaded.Fresh -> content(loaded.value, stale = false, previous)
                    is Loaded.Stale -> content(loaded.value, stale = true, previous)
                    is Loaded.Failed -> FacilityUiState.Error(loaded.error)
                }
                // Once per opening, from whichever answer arrives first.
                val shown = (loaded as? Loaded.Cached)?.value ?: (loaded as? Loaded.Fresh)?.value
                    ?: (loaded as? Loaded.Stale)?.value
                if (shown != null && !visitRecorded) {
                    visitRecorded = true
                    launch { recordVisit(shown) }
                    // Once per opening, beside the visit this already records, so the two
                    // cannot disagree about whether a facility was looked at.
                    analytics.track(AnalyticsEvent.FacilityView(facilityId = id))
                }
            }
            if (signedIn()) {
                facility.myRating(id).onSuccess { stars ->
                    (_state.value as? FacilityUiState.Content)?.let { _state.value = it.copy(myRating = stars) }
                }
            }
        }
    }

    private fun content(value: FacilityDetail, stale: Boolean, previous: FacilityUiState.Content?) =
        FacilityUiState.Content(value, stale, signedIn(), previous?.myRating)

    fun removeRating() {
        val current = _state.value as? FacilityUiState.Content ?: return
        viewModelScope.launch {
            facility.removeRating(id)
                .onSuccess {
                    _state.value = current.copy(myRating = null, ratingFailure = null)
                    // The average and count are the backend's; fetch them again.
                    refresh()
                }
                .onFailure { _state.value = current.copy(ratingFailure = it.toAppError()) }
        }
    }

    fun rate(stars: Int) {
        val current = _state.value as? FacilityUiState.Content ?: return
        viewModelScope.launch {
            facility.rate(id, stars)
                .onSuccess { stored ->
                    _state.value = current.copy(myRating = stored, ratingFailure = null)
                    // Counted only once the backend took it, so the number is ratings that
                    // exist rather than ratings that were attempted.
                    analytics.track(AnalyticsEvent.RatingSubmit(facilityId = id, stars = stored))
                    // The average and count are the backend's; fetch them again.
                    refresh()
                }
                .onFailure { _state.value = current.copy(ratingFailure = it.toAppError()) }
        }
    }
}
