package com.servacode.directory.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.servacode.directory.designsystem.generated.DirectoryTokens

private fun color(hex: String): Color = Color(hex.toColorInt())

/**
 * Tajawal, the token font (`typography.fontFamily.primary`), bundled with the app under its open
 * font licence (docs/design/fonts/Tajawal-OFL.txt). Arabic is the app's first language, so the
 * type it is set in belongs to the design system, not to a screen.
 */
private val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_medium, FontWeight.SemiBold),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

private val DirectoryColors = lightColorScheme(
    primary = color(DirectoryTokens.SemanticActionPrimary),
    onPrimary = color(DirectoryTokens.SemanticContentOnPrimary),
    primaryContainer = color(DirectoryTokens.SemanticSurfaceBrandSoft),
    onPrimaryContainer = color(DirectoryTokens.ColorsPrimaryStrong),
    secondary = color(DirectoryTokens.ColorsPrimaryStrong),
    onSecondary = color(DirectoryTokens.SemanticContentOnPrimary),
    secondaryContainer = color(DirectoryTokens.SemanticSurfaceBrandSoft),
    onSecondaryContainer = color(DirectoryTokens.ColorsPrimaryDeep),
    tertiary = color(DirectoryTokens.SemanticFeedbackInfo),
    onTertiary = color(DirectoryTokens.SemanticContentOnPrimary),
    background = color(DirectoryTokens.SemanticSurfaceCanvas),
    onBackground = color(DirectoryTokens.SemanticContentPrimary),
    surface = color(DirectoryTokens.SemanticSurfaceDefault),
    onSurface = color(DirectoryTokens.SemanticContentPrimary),
    surfaceVariant = color(DirectoryTokens.SemanticSurfaceSubtle),
    onSurfaceVariant = color(DirectoryTokens.SemanticContentSecondary),
    outline = color(DirectoryTokens.SemanticStrokeDefault),
    outlineVariant = color(DirectoryTokens.SemanticStrokeStrong),
    error = color(DirectoryTokens.SemanticFeedbackDanger),
    onError = color(DirectoryTokens.SemanticContentOnPrimary),
)

/** Brand colours that sit outside the Material colour scheme, from the same tokens. */
object BrandColors {
    /** The splash, system and app alike (brand_splash_background). */
    val splashBackground = color(DirectoryTokens.SemanticSurfaceDefault)
    val mark = color(DirectoryTokens.SemanticActionPrimary)
    val markDeep = color(DirectoryTokens.ColorsPrimaryDeep)
    val soft = color(DirectoryTokens.SemanticSurfaceBrandSoft)
    val softer = color(DirectoryTokens.ColorsPrimarySofter)
    val canvas = color(DirectoryTokens.SemanticSurfaceCanvas)
    val contentPrimary = color(DirectoryTokens.SemanticContentPrimary)
    val contentSecondary = color(DirectoryTokens.SemanticContentSecondary)
    val contentMuted = color(DirectoryTokens.SemanticContentMuted)
    val success = color(DirectoryTokens.SemanticFeedbackSuccess)
    val warning = color(DirectoryTokens.SemanticFeedbackWarning)
    val danger = color(DirectoryTokens.SemanticFeedbackDanger)
    val info = color(DirectoryTokens.SemanticFeedbackInfo)
    val stroke = color(DirectoryTokens.SemanticStrokeDefault)
}

private fun tajawal(size: Int, lineHeight: Int, weight: Int) = TextStyle(
    fontFamily = Tajawal,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private val DirectoryTypography = Typography(
    displaySmall = tajawal(
        DirectoryTokens.TypographyRolesDisplaySize,
        DirectoryTokens.TypographyRolesDisplayLineHeight,
        DirectoryTokens.TypographyRolesDisplayWeight,
    ),
    headlineLarge = tajawal(
        DirectoryTokens.TypographyRolesHeadlineLargeSize,
        DirectoryTokens.TypographyRolesHeadlineLargeLineHeight,
        DirectoryTokens.TypographyRolesHeadlineLargeWeight,
    ),
    headlineMedium = tajawal(
        DirectoryTokens.TypographyRolesHeadlineMediumSize,
        DirectoryTokens.TypographyRolesHeadlineMediumLineHeight,
        DirectoryTokens.TypographyRolesHeadlineMediumWeight,
    ),
    headlineSmall = tajawal(
        DirectoryTokens.TypographyRolesTitleLargeSize,
        DirectoryTokens.TypographyRolesTitleLargeLineHeight,
        DirectoryTokens.TypographyRolesTitleLargeWeight,
    ),
    titleLarge = tajawal(
        DirectoryTokens.TypographyRolesTitleLargeSize,
        DirectoryTokens.TypographyRolesTitleLargeLineHeight,
        DirectoryTokens.TypographyRolesTitleLargeWeight,
    ),
    titleMedium = tajawal(
        DirectoryTokens.TypographyRolesTitleMediumSize,
        DirectoryTokens.TypographyRolesTitleMediumLineHeight,
        DirectoryTokens.TypographyRolesTitleMediumWeight,
    ),
    titleSmall = tajawal(
        DirectoryTokens.TypographyRolesLabelLargeSize,
        DirectoryTokens.TypographyRolesLabelLargeLineHeight,
        DirectoryTokens.TypographyRolesLabelLargeWeight,
    ),
    bodyLarge = tajawal(
        DirectoryTokens.TypographyRolesBodyLargeSize,
        DirectoryTokens.TypographyRolesBodyLargeLineHeight,
        DirectoryTokens.TypographyRolesBodyLargeWeight,
    ),
    bodyMedium = tajawal(
        DirectoryTokens.TypographyRolesBodyMediumSize,
        DirectoryTokens.TypographyRolesBodyMediumLineHeight,
        DirectoryTokens.TypographyRolesBodyMediumWeight,
    ),
    bodySmall = tajawal(
        DirectoryTokens.TypographyRolesBodySmallSize,
        DirectoryTokens.TypographyRolesBodySmallLineHeight,
        DirectoryTokens.TypographyRolesBodySmallWeight,
    ),
    labelLarge = tajawal(
        DirectoryTokens.TypographyRolesLabelLargeSize,
        DirectoryTokens.TypographyRolesLabelLargeLineHeight,
        DirectoryTokens.TypographyRolesLabelLargeWeight,
    ),
    labelMedium = tajawal(
        DirectoryTokens.TypographyRolesLabelMediumSize,
        DirectoryTokens.TypographyRolesLabelMediumLineHeight,
        DirectoryTokens.TypographyRolesLabelMediumWeight,
    ),
    labelSmall = tajawal(
        DirectoryTokens.TypographyRolesLabelMediumSize,
        DirectoryTokens.TypographyRolesLabelMediumLineHeight,
        DirectoryTokens.TypographyRolesLabelMediumWeight,
    ),
)

/** The one extra role the Material set has no slot for: the brand's own display line. */
object DirectoryTextStyles {
    val display = DirectoryTypography.displaySmall
    val labelMedium = DirectoryTypography.labelMedium
}

private val DirectoryShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.small),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.xl),
)

@Composable
fun DirectoryTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val darkModeRequested = isSystemInDarkTheme()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DirectoryColors,
            typography = DirectoryTypography,
            shapes = DirectoryShapes,
            content = content,
        )
    }
}
