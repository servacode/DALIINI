package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val legal: LegalRepository,
) : ViewModel() {
    private val _pages = MutableStateFlow<LegalListState>(LegalListState.Loading)
    val pages: StateFlow<LegalListState> = _pages.asStateFlow()

    private val _page = MutableStateFlow<LegalPageState>(LegalPageState.Loading)
    val page: StateFlow<LegalPageState> = _page.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _pages.value = LegalListState.Loading
            _pages.value = legal.pages().fold(
                onSuccess = { LegalListState.Content(it) },
                onFailure = { LegalListState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    /** Opens one page. What was read before shows at once; the fetch only confirms it. */
    fun open(key: LegalPageKey) {
        legal.cached(key)?.let { _page.value = LegalPageState.Content(it) }
        viewModelScope.launch {
            _page.value = legal.page(key).fold(
                onSuccess = { LegalPageState.Content(it) },
                onFailure = { failure ->
                    legal.cached(key)?.let { LegalPageState.Content(it) }
                        ?: LegalPageState.Error(AppErrorText.of(failure.toAppError()))
                },
            )
        }
    }
}
