package com.servacode.directory.core.database

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.toAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** What a cache-first screen shows, in the order it can show it. */
sealed interface Loaded<out T> {
    /** From the cache, while the backend is being asked. */
    data class Cached<T>(val value: T) : Loaded<T>

    /** From the backend, and now in the cache. */
    data class Fresh<T>(val value: T) : Loaded<T>

    /** The backend could not be reached or refused; the cache is shown and marked as such. */
    data class Stale<T>(val value: T, val error: AppError) : Loaded<T>

    /** Nothing cached and nothing from the backend. */
    data class Failed(val error: AppError) : Loaded<Nothing>
}

/**
 * The cache-first order of `11-ANDROID-KOTLIN.md`: show what is cached, ask the backend,
 * store its answer, show it. When the backend fails the cache stays on screen, marked stale.
 *
 * A failed cache write does not fail the screen; the answer is still shown.
 */
fun <T : Any> cacheFirst(
    read: suspend () -> T?,
    fetch: suspend () -> T,
    write: suspend (T) -> Unit,
): Flow<Loaded<T>> = flow {
    val cached = try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    if (cached != null) emit(Loaded.Cached(cached))
    val fresh = try {
        fetch()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        val error = failure.toAppError()
        emit(if (cached != null) Loaded.Stale(cached, error) else Loaded.Failed(error))
        return@flow
    }
    try {
        write(fresh)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // The cache is an optimisation; the fresh answer is still the truth.
    }
    emit(Loaded.Fresh(fresh))
}
