package com.servacode.directory.core.designsystem

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servacode.directory.designsystem.generated.DirectoryTokens

/**
 * The measurements every screen uses, named once so no screen invents its own.
 *
 * Each value is a design token; nothing here is a number chosen at the call site.
 */
object Space {
    val xs = DirectoryTokens.SpacingXs.dp
    val sm = DirectoryTokens.SpacingSm.dp
    val md = DirectoryTokens.SpacingMd.dp
    val base = DirectoryTokens.SpacingBase.dp
    val lg = DirectoryTokens.SpacingLg.dp
    val xl = DirectoryTokens.SpacingXl.dp
    val xxl = DirectoryTokens.Spacing2xl.dp
    val xxxl = DirectoryTokens.Spacing3xl.dp
    val huge = DirectoryTokens.Spacing4xl.dp

    /** The side margin of every screen. */
    val screen = DirectoryTokens.SpacingBase.dp
}

object Radius {
    val small = DirectoryTokens.RadiusSmall.dp
    val medium = DirectoryTokens.RadiusMedium.dp
    val large = DirectoryTokens.RadiusLarge.dp
    val xl = DirectoryTokens.RadiusXl.dp
    val pill = DirectoryTokens.RadiusPill.dp
}

object Elevation {
    val none = DirectoryTokens.ElevationNoneY.dp
    val low = DirectoryTokens.ElevationLowY.dp
    val medium = DirectoryTokens.ElevationMediumY.dp
}

/**
 * The type scale, in scalable pixels.
 *
 * Compose screens take their sizes from the Material typography the theme builds out of these
 * same tokens, and never need this. The home-screen widget does: Glance has its own text style
 * and cannot read the app's theme, so without a named scale it was choosing 11, 12, 13, 14 and
 * 15 at its call sites — five sizes, none of them on the scale the rest of the product uses.
 */
object TypeScale {
    val display = DirectoryTokens.TypographyRolesDisplaySize.sp
    val headlineLarge = DirectoryTokens.TypographyRolesHeadlineLargeSize.sp
    val headlineMedium = DirectoryTokens.TypographyRolesHeadlineMediumSize.sp
    val titleLarge = DirectoryTokens.TypographyRolesTitleLargeSize.sp
    val titleMedium = DirectoryTokens.TypographyRolesTitleMediumSize.sp
    val bodyLarge = DirectoryTokens.TypographyRolesBodyLargeSize.sp
    val bodyMedium = DirectoryTokens.TypographyRolesBodyMediumSize.sp
    val bodySmall = DirectoryTokens.TypographyRolesBodySmallSize.sp
    val labelLarge = DirectoryTokens.TypographyRolesLabelLargeSize.sp
    val labelMedium = DirectoryTokens.TypographyRolesLabelMediumSize.sp
}

/** Icons come in three sizes, and nothing else. */
object IconSize {
    val small = 18.dp
    val medium = 22.dp
    val large = 28.dp
}

/** How tall the things a finger presses are, so every target clears the accessible minimum. */
object Sizes {
    val button = 52.dp
    val field = 56.dp
    val touchTarget = 48.dp
    val thumbnail = 72.dp
    val categoryCircle = 56.dp

    /** The app's own bottom bar, without the system's own strip beneath it. */
    val bottomBar = 64.dp

    /** How far the map's own controls sit above the card that names a chosen marker. */
    val mapCardClearance = 148.dp

    /** Wide enough for the longest category name the taxonomy holds, over two lines. */
    val categoryLabel = 92.dp

    /** A filter chip that has to share a phone's width with three others. */
    val compactChip = 40.dp
    val avatar = 64.dp

    /** A card the user swipes sideways through, and the picture inside it. */
    val banner = 280.dp
    val bannerImage = 132.dp

    /** The picture a facility opens with, and the circles of its actions under it. */
    val hero = 260.dp
    val actionCircle = 52.dp

    /**
     * How much of the map's height the category rail may take before it scrolls.
     *
     * Enough for the five sections a province opens with, now that each carries its name as
     * well as its mark. Past that it scrolls rather than growing, so a province that adds
     * sections never takes the map.
     */
    val railMaxHeight = 440.dp
}
