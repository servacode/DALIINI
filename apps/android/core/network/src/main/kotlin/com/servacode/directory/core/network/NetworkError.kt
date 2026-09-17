package com.servacode.directory.core.network

sealed interface NetworkError {
    data object Offline : NetworkError
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object NotFound : NetworkError
    data object RateLimited : NetworkError
    data class Server(val statusCode: Int) : NetworkError
    data object Unexpected : NetworkError
}

object NetworkErrorMapper {
    fun fromStatus(statusCode: Int): NetworkError = when (statusCode) {
        401 -> NetworkError.Unauthorized
        403 -> NetworkError.Forbidden
        404 -> NetworkError.NotFound
        429 -> NetworkError.RateLimited
        in 500..599 -> NetworkError.Server(statusCode)
        else -> NetworkError.Unexpected
    }
}
