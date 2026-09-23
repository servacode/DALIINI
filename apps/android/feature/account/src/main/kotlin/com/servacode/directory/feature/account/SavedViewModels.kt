package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.InboxMessage
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Content(
        val items: List<FacilitySummary>,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val saved: SavedRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()
    private var nextCursor: String? = null
    private var loading: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loading?.cancel()
        nextCursor = null
        loading = viewModelScope.launch {
            _state.value = FavoritesUiState.Loading
            _state.value = saved.favorites().fold(
                onSuccess = { page ->
                    nextCursor = page.nextCursor
                    FavoritesUiState.Content(items = page.items, hasMore = page.hasMore)
                },
                onFailure = { FavoritesUiState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun loadMore() {
        val current = _state.value as? FavoritesUiState.Content ?: return
        val cursor = nextCursor ?: return
        if (current.loadingMore) return
        _state.value = current.copy(loadingMore = true, moreError = null)
        loading = viewModelScope.launch {
            saved.favorites(cursor)
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.value = current.copy(
                        items = current.items + page.items,
                        hasMore = page.hasMore,
                        loadingMore = false,
                    )
                }
                .onFailure {
                    _state.value = current.copy(
                        loadingMore = false,
                        moreError = AppErrorText.of(it.toAppError()),
                    )
                }
        }
    }

    /**
     * Unsaving from this very list takes the row out at once and puts it back if the backend
     * refuses. The list is the account's own, so the answer it shows must be the account's.
     */
    fun unsave(facilityId: String) {
        val current = _state.value as? FavoritesUiState.Content ?: return
        val without = current.items.filterNot { it.id == facilityId }
        _state.value = current.copy(items = without)
        viewModelScope.launch {
            saved.unsave(facilityId).onFailure { _state.value = current }
        }
    }
}

sealed interface InboxUiState {
    data object Loading : InboxUiState
    data class Content(
        val items: List<InboxMessage>,
        val unreadCount: Int,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : InboxUiState
    data class Error(val message: String) : InboxUiState
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val saved: SavedRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val state: StateFlow<InboxUiState> = _state.asStateFlow()
    private var nextCursor: String? = null
    private var loading: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loading?.cancel()
        nextCursor = null
        loading = viewModelScope.launch {
            _state.value = InboxUiState.Loading
            _state.value = saved.inbox().fold(
                onSuccess = { page ->
                    nextCursor = page.nextCursor
                    InboxUiState.Content(
                        items = page.items,
                        unreadCount = page.unreadCount,
                        hasMore = page.hasMore,
                    )
                },
                onFailure = { InboxUiState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun loadMore() {
        val current = _state.value as? InboxUiState.Content ?: return
        val cursor = nextCursor ?: return
        if (current.loadingMore) return
        _state.value = current.copy(loadingMore = true, moreError = null)
        loading = viewModelScope.launch {
            saved.inbox(cursor)
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.value = current.copy(
                        items = current.items + page.items,
                        hasMore = page.hasMore,
                        loadingMore = false,
                    )
                }
                .onFailure {
                    _state.value = current.copy(
                        loadingMore = false,
                        moreError = AppErrorText.of(it.toAppError()),
                    )
                }
        }
    }

    /** Opening a message reads it: the row changes at once, and the count with it. */
    fun read(messageId: String) {
        val current = _state.value as? InboxUiState.Content ?: return
        val message = current.items.firstOrNull { it.id == messageId } ?: return
        if (message.isRead) return
        _state.value = current.copy(
            items = current.items.map { if (it.id == messageId) it.copy(isRead = true) else it },
            unreadCount = (current.unreadCount - 1).coerceAtLeast(0),
        )
        viewModelScope.launch {
            saved.markRead(messageId).onFailure { _state.value = current }
        }
    }

    fun readAll() {
        val current = _state.value as? InboxUiState.Content ?: return
        if (current.unreadCount == 0) return
        _state.value = current.copy(
            items = current.items.map { it.copy(isRead = true) },
            unreadCount = 0,
        )
        viewModelScope.launch {
            saved.markAllRead().onFailure { _state.value = current }
        }
    }
}
