package com.servacode.directory.core.designsystem

import android.app.Activity
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Android's part of the theme. Resources follow the theme's choice: the composition reads them
 * through a configuration whose night bit is the theme's, so the token colours in `values-night`
 * — and what a screen still draws from Android resources with them — are the dark ones even when
 * the phone itself is light. And the system bars' icons are light on the dark theme, dark on the
 * light one.
 */
@Composable
internal actual fun PlatformTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val night = if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    val themed = remember(context, configuration, night) {
        if (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == night) {
            context to configuration
        } else {
            val override = Configuration(configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
            }
            // A wrapper, not createConfigurationContext: it still is the activity for everything
            // but its resources, so starting a dialer or a browser from it needs no new task.
            // Its own theme object, copied from the activity's: the vector cache is keyed by theme,
            // so a shared one would hand back an icon already drawn in the other mode's colours.
            val wrapped = ContextThemeWrapper(context, 0).apply {
                applyOverrideConfiguration(override)
                theme.setTo(context.theme)
            }
            wrapped to override
        }
    }
    SystemBarAppearance(darkTheme)
    CompositionLocalProvider(
        LocalContext provides themed.first,
        LocalConfiguration provides themed.second,
        // stringResource and painterResource read this one, not the context.
        LocalResources provides themed.first.resources,
        content = content,
    )
}

/** Light system-bar icons on the dark theme, dark ones on the light theme. */
@Composable
private fun SystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
