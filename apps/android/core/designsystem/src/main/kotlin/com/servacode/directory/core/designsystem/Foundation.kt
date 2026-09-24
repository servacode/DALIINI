package com.servacode.directory.core.designsystem

import androidx.compose.ui.unit.dp
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

    /** How much of the map's height the category rail may take before it scrolls. */
    val railMaxHeight = 320.dp
}
