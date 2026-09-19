package com.servacode.directory.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Paints the status and navigation bars [color], with dark icons, while this is composed, and
 * puts back what was there when it leaves. For a screen whose background has to run under the
 * bars without a seam, as the splash does after the system splash. Other screens keep the bars
 * the app theme gives them.
 *
 * Where the app draws edge to edge (enforced from Android 15 for this target), the bars are
 * transparent and the screen's own background already shows through; only the icons change.
 */
@Composable
fun SystemBarsColor(color: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, color) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val argb = color.toArgb()
            @Suppress("DEPRECATION")
            val statusBar = window.statusBarColor
            @Suppress("DEPRECATION")
            val navigationBar = window.navigationBarColor
            val lightStatusBar = controller.isAppearanceLightStatusBars
            val lightNavigationBar = controller.isAppearanceLightNavigationBars
            // Only what differs: an appearance change relays the window out, which the splash's
            // first frame would otherwise pay for.
            @Suppress("DEPRECATION")
            if (statusBar != argb) window.statusBarColor = argb
            @Suppress("DEPRECATION")
            if (navigationBar != argb) window.navigationBarColor = argb
            if (!lightStatusBar) controller.isAppearanceLightStatusBars = true
            if (!lightNavigationBar) controller.isAppearanceLightNavigationBars = true
            onDispose {
                @Suppress("DEPRECATION")
                if (statusBar != argb) window.statusBarColor = statusBar
                @Suppress("DEPRECATION")
                if (navigationBar != argb) window.navigationBarColor = navigationBar
                if (!lightStatusBar) controller.isAppearanceLightStatusBars = false
                if (!lightNavigationBar) controller.isAppearanceLightNavigationBars = false
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
