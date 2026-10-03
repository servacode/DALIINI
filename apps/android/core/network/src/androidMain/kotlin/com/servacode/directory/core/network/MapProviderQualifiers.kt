package com.servacode.directory.core.network

import javax.inject.Qualifier

/**
 * The HTTP client the map providers share, told apart from the one the API is served over.
 *
 * It lives in a file of its own so that a provider can be compiled and tested without the
 * Android framework: the adapters beside it are not all platform-free, and an annotation is no
 * reason for a routing engine's parsing to go untested.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapProviderHttpClient
