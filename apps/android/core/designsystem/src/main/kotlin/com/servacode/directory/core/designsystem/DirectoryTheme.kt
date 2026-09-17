package com.servacode.directory.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.servacode.directory.designsystem.generated.DirectoryTokens

private fun color(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

private val DirectoryColors = lightColorScheme(
    primary = color(DirectoryTokens.SemanticActionPrimary),
    onPrimary = color(DirectoryTokens.SemanticContentOnPrimary),
    background = color(DirectoryTokens.SemanticSurfaceCanvas),
    onBackground = color(DirectoryTokens.SemanticContentPrimary),
    surface = color(DirectoryTokens.SemanticSurfaceDefault),
    onSurface = color(DirectoryTokens.SemanticContentPrimary),
    error = color(DirectoryTokens.SemanticFeedbackDanger),
)

private val DirectoryTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = DirectoryTokens.TypographyRolesHeadlineLargeSize.sp,
        lineHeight = DirectoryTokens.TypographyRolesHeadlineLargeLineHeight.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = DirectoryTokens.TypographyRolesBodyLargeSize.sp,
        lineHeight = DirectoryTokens.TypographyRolesBodyLargeLineHeight.sp,
    ),
)

@Composable
fun DirectoryTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val darkModeRequested = isSystemInDarkTheme()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DirectoryColors,
            typography = DirectoryTypography,
            content = content,
        )
    }
}
