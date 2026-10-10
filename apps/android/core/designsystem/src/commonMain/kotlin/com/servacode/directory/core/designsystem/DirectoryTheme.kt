package com.servacode.directory.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.servacode.directory.designsystem.generated.DirectoryTokens
import org.jetbrains.compose.resources.Font

/** A token's colour, written `#RRGGBB` or `#AARRGGBB`. */
internal fun hexColor(hex: String): Color {
    val digits = hex.removePrefix("#")
    val value = digits.toLong(16)
    return when (digits.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> throw IllegalArgumentException("Not a colour: $hex")
    }
}

private fun color(hex: String): Color = hexColor(hex)

/**
 * The brand's two faces, read from the design-token package (`packages/design-tokens/fonts`,
 * SIL OFL) so the app sets the same files as the site and the console (DECISION-060).
 *
 * IBM Plex Sans Arabic (`typography.fontFamily.primary`) sets everything read: body, labels,
 * fields, lists. It has the four weights the scale uses, 400 to 700, so nothing is substituted.
 *
 * The faces are Compose resources (DECISION-094), read the same way on Android and on the
 * iPhone; a font is loaded in composition, so the families are made there.
 */
@Composable
fun plexArabic(): FontFamily = FontFamily(
    Font(Res.font.plex_arabic_regular, FontWeight.Normal),
    Font(Res.font.plex_arabic_medium, FontWeight.Medium),
    Font(Res.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(Res.font.plex_arabic_bold, FontWeight.Bold),
)

/**
 * Alexandria (`typography.fontFamily.display`) sets titles and the big numbers: the roles whose
 * token says `family: display`. Used with restraint, it is what gives a screen its voice.
 */
@Composable
fun alexandria(): FontFamily = FontFamily(
    Font(Res.font.alexandria_semibold, FontWeight.SemiBold),
    Font(Res.font.alexandria_bold, FontWeight.Bold),
    Font(Res.font.alexandria_extrabold, FontWeight.ExtraBold),
)

/**
 * The colours of one theme.
 *
 * Every one of them is a design token; none is written here. A second theme is therefore a
 * second object in this file and nothing else — no screen names a colour, so no screen has to
 * be found and edited when the app wears a different one.
 */
object DirectoryPalettes {
    /** The app as it ships: the brand's greens on paper. */
    val light: ColorScheme = lightColorScheme(
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

    /**
     * The same brand at night, from the approved dark tokens (`semanticDark` / `colorsDark`).
     *
     * Contrast, as the tokens give it: primary text on the canvas and on cards is above 12:1,
     * secondary text on the subtle surface above 7:1, and the green action carries its near-black
     * label at about 9:1. The container roles are set too — left to Material they are a violet
     * grey that has nothing to do with this brand.
     */
    val dark: ColorScheme = darkColorScheme(
        primary = color(DirectoryTokens.SemanticDarkActionPrimary),
        onPrimary = color(DirectoryTokens.SemanticDarkContentOnPrimary),
        primaryContainer = color(DirectoryTokens.SemanticDarkSurfaceBrandSoft),
        onPrimaryContainer = color(DirectoryTokens.ColorsDarkPrimaryStrong),
        inversePrimary = color(DirectoryTokens.SemanticActionPrimary),
        secondary = color(DirectoryTokens.ColorsDarkPrimaryStrong),
        onSecondary = color(DirectoryTokens.ColorsDarkOnPrimary),
        secondaryContainer = color(DirectoryTokens.SemanticDarkSurfaceBrandSoft),
        onSecondaryContainer = color(DirectoryTokens.SemanticDarkContentPrimary),
        tertiary = color(DirectoryTokens.SemanticDarkFeedbackInfo),
        onTertiary = color(DirectoryTokens.ColorsDarkOnPrimary),
        tertiaryContainer = color(DirectoryTokens.SemanticDarkFeedbackInfoSoft),
        onTertiaryContainer = color(DirectoryTokens.SemanticDarkFeedbackInfo),
        background = color(DirectoryTokens.SemanticDarkSurfaceCanvas),
        onBackground = color(DirectoryTokens.SemanticDarkContentPrimary),
        surface = color(DirectoryTokens.SemanticDarkSurfaceDefault),
        onSurface = color(DirectoryTokens.SemanticDarkContentPrimary),
        surfaceVariant = color(DirectoryTokens.SemanticDarkSurfaceSubtle),
        onSurfaceVariant = color(DirectoryTokens.SemanticDarkContentSecondary),
        surfaceTint = color(DirectoryTokens.SemanticDarkActionPrimary),
        inverseSurface = color(DirectoryTokens.SemanticDarkContentPrimary),
        inverseOnSurface = color(DirectoryTokens.SemanticDarkSurfaceCanvas),
        outline = color(DirectoryTokens.SemanticDarkStrokeDefault),
        outlineVariant = color(DirectoryTokens.SemanticDarkStrokeStrong),
        error = color(DirectoryTokens.SemanticDarkFeedbackDanger),
        onError = color(DirectoryTokens.ColorsDarkOnPrimary),
        errorContainer = color(DirectoryTokens.SemanticDarkFeedbackDangerSoft),
        onErrorContainer = color(DirectoryTokens.SemanticDarkContentDanger),
        surfaceBright = color(DirectoryTokens.SemanticDarkSurfaceHover),
        surfaceDim = color(DirectoryTokens.SemanticDarkSurfaceCanvas),
        surfaceContainerLowest = color(DirectoryTokens.SemanticDarkSurfaceCanvas),
        surfaceContainerLow = color(DirectoryTokens.SemanticDarkSurfaceDefault),
        surfaceContainer = color(DirectoryTokens.SemanticDarkSurfaceDefault),
        surfaceContainerHigh = color(DirectoryTokens.SemanticDarkSurfaceSubtle),
        surfaceContainerHighest = color(DirectoryTokens.SemanticDarkSurfaceHover),
        scrim = color(DirectoryTokens.ColorsDarkBarDeep),
    )
}

/**
 * The colours a state is shown in: one tone per state, as the shared vocabulary assigns it
 * (`vocabulary.json`), so a word means the same colour in the app, the site and the console.
 * [content] is the word and its dot; [container] is the soft ground under them.
 */
@Immutable
data class ToneColors(val content: Color, val container: Color)

/** Every tone, for one theme. */
@Immutable
data class DirectoryToneColors(
    val neutral: ToneColors,
    val positive: ToneColors,
    val warning: ToneColors,
    val danger: ToneColors,
    val info: ToneColors,
    val brand: ToneColors,
    val accent: ToneColors,
) {
    fun of(tone: StatusTone): ToneColors = when (tone) {
        StatusTone.NEUTRAL -> neutral
        StatusTone.POSITIVE -> positive
        StatusTone.WARNING -> warning
        StatusTone.DANGER -> danger
        StatusTone.INFO -> info
        StatusTone.BRAND -> brand
        StatusTone.ACCENT -> accent
    }

    companion object {
        val light = DirectoryToneColors(
            neutral = ToneColors(
                color(DirectoryTokens.SemanticContentSecondary),
                color(DirectoryTokens.SemanticSurfaceSubtle),
            ),
            positive = ToneColors(
                color(DirectoryTokens.SemanticFeedbackSuccess),
                color(DirectoryTokens.SemanticFeedbackSuccessSoft),
            ),
            // content.warning, not feedback.warning: the amber is for fills, and too light to
            // carry a word on white.
            warning = ToneColors(
                color(DirectoryTokens.SemanticContentWarning),
                color(DirectoryTokens.SemanticFeedbackWarningSoft),
            ),
            danger = ToneColors(
                color(DirectoryTokens.SemanticContentDanger),
                color(DirectoryTokens.SemanticFeedbackDangerSoft),
            ),
            info = ToneColors(
                color(DirectoryTokens.SemanticFeedbackInfo),
                color(DirectoryTokens.SemanticFeedbackInfoSoft),
            ),
            brand = ToneColors(
                color(DirectoryTokens.SemanticActionPrimary),
                color(DirectoryTokens.SemanticSurfaceBrandSoft),
            ),
            // Gold, for on duty now alone: the darker content gold, because the fill gold is
            // too light to carry a word on its own soft ground.
            accent = ToneColors(
                color(DirectoryTokens.SemanticAccentContent),
                color(DirectoryTokens.SemanticAccentSoft),
            ),
        )

        val dark = DirectoryToneColors(
            neutral = ToneColors(
                color(DirectoryTokens.SemanticDarkContentSecondary),
                color(DirectoryTokens.SemanticDarkSurfaceSubtle),
            ),
            positive = ToneColors(
                color(DirectoryTokens.SemanticDarkFeedbackSuccess),
                color(DirectoryTokens.SemanticDarkFeedbackSuccessSoft),
            ),
            warning = ToneColors(
                color(DirectoryTokens.SemanticDarkContentWarning),
                color(DirectoryTokens.SemanticDarkFeedbackWarningSoft),
            ),
            danger = ToneColors(
                color(DirectoryTokens.SemanticDarkContentDanger),
                color(DirectoryTokens.SemanticDarkFeedbackDangerSoft),
            ),
            info = ToneColors(
                color(DirectoryTokens.SemanticDarkFeedbackInfo),
                color(DirectoryTokens.SemanticDarkFeedbackInfoSoft),
            ),
            brand = ToneColors(
                color(DirectoryTokens.SemanticDarkActionPrimary),
                color(DirectoryTokens.SemanticDarkSurfaceBrandSoft),
            ),
            accent = ToneColors(
                color(DirectoryTokens.SemanticDarkAccentContent),
                color(DirectoryTokens.SemanticDarkAccentSoft),
            ),
        )
    }
}

/** The tones of the theme in use. */
val LocalDirectoryTones = staticCompositionLocalOf { DirectoryToneColors.light }

/** Brand colours that sit outside the Material colour scheme, from the same tokens. */
object BrandColors {
    /** The splash, system and app alike (brand_splash_background). */
    val splashBackground = color(DirectoryTokens.SemanticSurfaceDefault)
    val mark = color(DirectoryTokens.SemanticActionPrimary)
    val markDeep = color(DirectoryTokens.ColorsPrimaryDeep)
    val soft = color(DirectoryTokens.SemanticSurfaceBrandSoft)
    val softer = color(DirectoryTokens.ColorsPrimarySofter)

    /**
     * [soft] and [softer] in the theme in use. The light values are paper-pale greens; under the
     * dark theme they would be bright slabs that the theme's light words vanish into.
     */
    val softNow: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalDirectoryDark.current) darkSoft else soft
    val softerNow: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalDirectoryDark.current) darkSofter else softer
    private val darkSoft = color(DirectoryTokens.ColorsDarkPrimarySoft)
    private val darkSofter = color(DirectoryTokens.ColorsDarkPrimarySofter)
    val canvas = color(DirectoryTokens.SemanticSurfaceCanvas)
    val contentPrimary = color(DirectoryTokens.SemanticContentPrimary)
    val contentSecondary = color(DirectoryTokens.SemanticContentSecondary)
    val contentMuted = color(DirectoryTokens.SemanticContentMuted)
    val success = color(DirectoryTokens.SemanticFeedbackSuccess)
    val warning = color(DirectoryTokens.SemanticFeedbackWarning)
    val danger = color(DirectoryTokens.SemanticFeedbackDanger)
    val info = color(DirectoryTokens.SemanticFeedbackInfo)
    val stroke = color(DirectoryTokens.SemanticStrokeDefault)

    /** Gold: on duty now, and the one solid button that leads to it. Dark words on it. */
    val accent = color(DirectoryTokens.SemanticAccentDefault)
    val onAccent = color(DirectoryTokens.SemanticAccentOnAccent)

    /**
     * The app's two bars, top and bottom, and what may be written on them.
     *
     * One token, used in both places, so they cannot drift a shade apart — which is the sort of
     * difference nobody can name but everybody sees. Defined once in the token set; nothing in
     * the app writes this value itself.
     */
    val bar = color(DirectoryTokens.SemanticSurfaceBar)
    val onBar = color(DirectoryTokens.SemanticContentOnBar)
    val onBarMuted = color(DirectoryTokens.SemanticContentOnBarMuted)
}

/** The two faces, made once per composition of the theme. */
@Immutable
private class Faces(val plex: FontFamily, val display: FontFamily) {
    /** The face a role's token names: `display` is Alexandria, anything else Plex. */
    fun of(family: String): FontFamily = if (family == "display") display else plex
}

private fun Faces.role(family: String, size: Int, lineHeight: Int, weight: Int) = TextStyle(
    fontFamily = of(family),
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private fun directoryTypography(faces: Faces) = with(faces) { Typography(
    displaySmall = role(
        DirectoryTokens.TypographyRolesDisplayFamily,
        DirectoryTokens.TypographyRolesDisplaySize,
        DirectoryTokens.TypographyRolesDisplayLineHeight,
        DirectoryTokens.TypographyRolesDisplayWeight,
    ),
    headlineLarge = role(
        DirectoryTokens.TypographyRolesHeadlineLargeFamily,
        DirectoryTokens.TypographyRolesHeadlineLargeSize,
        DirectoryTokens.TypographyRolesHeadlineLargeLineHeight,
        DirectoryTokens.TypographyRolesHeadlineLargeWeight,
    ),
    headlineMedium = role(
        DirectoryTokens.TypographyRolesHeadlineMediumFamily,
        DirectoryTokens.TypographyRolesHeadlineMediumSize,
        DirectoryTokens.TypographyRolesHeadlineMediumLineHeight,
        DirectoryTokens.TypographyRolesHeadlineMediumWeight,
    ),
    headlineSmall = role(
        DirectoryTokens.TypographyRolesTitleLargeFamily,
        DirectoryTokens.TypographyRolesTitleLargeSize,
        DirectoryTokens.TypographyRolesTitleLargeLineHeight,
        DirectoryTokens.TypographyRolesTitleLargeWeight,
    ),
    titleLarge = role(
        DirectoryTokens.TypographyRolesTitleLargeFamily,
        DirectoryTokens.TypographyRolesTitleLargeSize,
        DirectoryTokens.TypographyRolesTitleLargeLineHeight,
        DirectoryTokens.TypographyRolesTitleLargeWeight,
    ),
    titleMedium = role(
        DirectoryTokens.TypographyRolesTitleMediumFamily,
        DirectoryTokens.TypographyRolesTitleMediumSize,
        DirectoryTokens.TypographyRolesTitleMediumLineHeight,
        DirectoryTokens.TypographyRolesTitleMediumWeight,
    ),
    titleSmall = role(
        DirectoryTokens.TypographyRolesLabelLargeFamily,
        DirectoryTokens.TypographyRolesLabelLargeSize,
        DirectoryTokens.TypographyRolesLabelLargeLineHeight,
        DirectoryTokens.TypographyRolesLabelLargeWeight,
    ),
    bodyLarge = role(
        DirectoryTokens.TypographyRolesBodyLargeFamily,
        DirectoryTokens.TypographyRolesBodyLargeSize,
        DirectoryTokens.TypographyRolesBodyLargeLineHeight,
        DirectoryTokens.TypographyRolesBodyLargeWeight,
    ),
    bodyMedium = role(
        DirectoryTokens.TypographyRolesBodyMediumFamily,
        DirectoryTokens.TypographyRolesBodyMediumSize,
        DirectoryTokens.TypographyRolesBodyMediumLineHeight,
        DirectoryTokens.TypographyRolesBodyMediumWeight,
    ),
    bodySmall = role(
        DirectoryTokens.TypographyRolesBodySmallFamily,
        DirectoryTokens.TypographyRolesBodySmallSize,
        DirectoryTokens.TypographyRolesBodySmallLineHeight,
        DirectoryTokens.TypographyRolesBodySmallWeight,
    ),
    labelLarge = role(
        DirectoryTokens.TypographyRolesLabelLargeFamily,
        DirectoryTokens.TypographyRolesLabelLargeSize,
        DirectoryTokens.TypographyRolesLabelLargeLineHeight,
        DirectoryTokens.TypographyRolesLabelLargeWeight,
    ),
    labelMedium = role(
        DirectoryTokens.TypographyRolesLabelMediumFamily,
        DirectoryTokens.TypographyRolesLabelMediumSize,
        DirectoryTokens.TypographyRolesLabelMediumLineHeight,
        DirectoryTokens.TypographyRolesLabelMediumWeight,
    ),
    labelSmall = role(
        DirectoryTokens.TypographyRolesLabelMediumFamily,
        DirectoryTokens.TypographyRolesLabelMediumSize,
        DirectoryTokens.TypographyRolesLabelMediumLineHeight,
        DirectoryTokens.TypographyRolesLabelMediumWeight,
    ),
) }

/** The one extra role the Material set has no slot for: the brand's own display line. */
object DirectoryTextStyles {
    val display: TextStyle @Composable get() = MaterialTheme.typography.displaySmall
    val labelMedium: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
}

private val DirectoryShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.small),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/** Whether the theme in use is the dark one; what the icons and illustrations are drawn for. */
val LocalDirectoryDark = staticCompositionLocalOf { false }

/**
 * The app's theme, and the one place a different one is chosen.
 *
 * [darkTheme] defaults to the phone's own setting, so a reader who keeps their device dark is
 * met in the dark; the app's own setting (تلقائي / فاتح / داكن) passes it explicitly, as a
 * screenshot or a preview does. No screen knows that themes exist.
 *
 * The icons and illustrations follow the same choice, because they are drawn in the theme's
 * colours ([LocalDirectoryDark]). What else a platform needs for it — on Android, resources read
 * in the theme's night mode and system bars that match — is [PlatformTheme].
 */
@Composable
fun DirectoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val plex = plexArabic()
    val display = alexandria()
    val typography = remember(plex, display) { directoryTypography(Faces(plex, display)) }
    PlatformTheme(darkTheme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDirectoryDark provides darkTheme,
            LocalDirectoryTones provides if (darkTheme) DirectoryToneColors.dark else DirectoryToneColors.light,
        ) {
            MaterialTheme(
                colorScheme = if (darkTheme) DirectoryPalettes.dark else DirectoryPalettes.light,
                typography = typography,
                shapes = DirectoryShapes,
                content = content,
            )
        }
    }
}

/** What a platform adds around the theme for [darkTheme]; see each platform's own. */
@Composable
internal expect fun PlatformTheme(darkTheme: Boolean, content: @Composable () -> Unit)
