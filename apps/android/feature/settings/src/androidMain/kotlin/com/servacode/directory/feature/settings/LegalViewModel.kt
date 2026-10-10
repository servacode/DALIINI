package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.FaqEntry
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.SupportContact
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val legal: LegalRepository,
) : ViewModel() {
    private val _pages = MutableStateFlow<LegalListState>(LegalListState.Loading)
    val pages: StateFlow<LegalListState> = _pages.asStateFlow()

    // The support row is an extra, never a reason for the screen to fail: unreadable or not
    // configured, it is simply not shown.
    private val _support = MutableStateFlow<SupportContact?>(null)
    val support: StateFlow<SupportContact?> = _support.asStateFlow()

    private val _page = MutableStateFlow<LegalPageState>(LegalPageState.Loading)
    val page: StateFlow<LegalPageState> = _page.asStateFlow()

    // The console's questions, for the FAQ page. Empty, or unreadable, the page's own text shows.
    private val _faq = MutableStateFlow<List<FaqEntry>>(emptyList())
    val faq: StateFlow<List<FaqEntry>> = _faq.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _support.value = legal.support() }
        viewModelScope.launch {
            _pages.value = LegalListState.Loading
            _pages.value = legal.pages().fold(
                onSuccess = { LegalListState.Content(it) },
                onFailure = { LegalListState.Error(it.toAppError()) },
            )
        }
    }

    /** Opens one page. What was read before shows at once; the fetch only confirms it. */
    fun open(key: LegalPageKey) {
        if (key == LegalPageKey.FAQ) {
            viewModelScope.launch { _faq.value = legal.faq().getOrDefault(emptyList()) }
        }
        legal.cached(key)?.let { _page.value = LegalPageState.Content(it) }
        viewModelScope.launch {
            _page.value = legal.page(key).fold(
                onSuccess = { LegalPageState.Content(it) },
                onFailure = { failure ->
                    legal.cached(key)?.let { LegalPageState.Content(it) }
                        ?: LegalPageState.Error(failure.toAppError())
                },
            )
        }
    }
}
