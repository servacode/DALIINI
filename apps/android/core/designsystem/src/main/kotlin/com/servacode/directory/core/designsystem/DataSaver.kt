package com.servacode.directory.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * «توفير البيانات» as the screens read it: provided once at the app's root from the reader's
 * setting. While it is on, list thumbnails are not fetched (the brand's placeholder stands in),
 * a photo gallery loads only the photo on screen, and the advertisement slider is not shown.
 *
 * The backend serves each picture at one size — there is no smaller rendition to ask for — so the
 * saving is in pictures not fetched at all rather than in smaller ones.
 */
val LocalDataSaver = staticCompositionLocalOf { false }
