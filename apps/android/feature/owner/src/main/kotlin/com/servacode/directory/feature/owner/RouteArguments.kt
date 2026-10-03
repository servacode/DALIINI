package com.servacode.directory.feature.owner

import androidx.lifecycle.SavedStateHandle

/**
 * The `id` of the route a screen was opened with: [com.servacode.directory.core.model.DirectoryRoute.ManageFacility]
 * or [com.servacode.directory.core.model.DirectoryRoute.Claim].
 *
 * Type-safe navigation keeps each route argument in the saved state under its property's name,
 * which is what `toRoute` reads too. Reading the one string directly does the same without
 * going through a Bundle, so the view models that need it also run in plain JVM tests.
 */
internal fun SavedStateHandle.routeId(): String =
    checkNotNull(get<String>("id")) { "The route carries no id." }
