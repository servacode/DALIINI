package com.servacode.directory.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object ProvinceRequired : HomeUiState
    data class Content(val snapshot: HomeSnapshot, val stale: Boolean) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHome: HomeUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                val province = (_state.value as? HomeUiState.Content)?.snapshot?.province?.id
                if (RealtimeInvalidation.refreshesProvinceLists(event, province)) refresh()
            }
        }
    }

    fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            if (_state.value !is HomeUiState.Content) _state.value = HomeUiState.Loading
            loadHome().collect { load ->
                _state.value = when (load) {
                    HomeLoad.ProvinceRequired -> HomeUiState.ProvinceRequired
                    is HomeLoad.Snapshot -> when (val loaded = load.loaded) {
                        is Loaded.Cached -> HomeUiState.Content(loaded.value, stale = false)
                        is Loaded.Fresh -> HomeUiState.Content(loaded.value, stale = false)
                        is Loaded.Stale -> HomeUiState.Content(loaded.value, stale = true)
                        is Loaded.Failed -> HomeUiState.Error(AppErrorText.of(loaded.error))
                    }
                }
            }
        }
    }
}
