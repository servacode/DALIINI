package com.servacode.directory.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servacode.directory.designsystem.generated.DirectoryIconPaths
import com.servacode.directory.designsystem.generated.DirectoryTokens
import com.servacode.directory.designsystem.generated.IconPaths

/** The colour of the theme a path of an icon is drawn in when the icon is not tinted. */
internal enum class GlyphRole { CONTENT, DANGER, ON_PRIMARY }

/** One path of an icon: filled, or stroked at [width] with round or square ends and corners. */
@Immutable
internal class GlyphPath private constructor(
    val data: String,
    val role: GlyphRole,
    val filled: Boolean,
    val width: Float,
    val roundCap: Boolean,
    val roundJoin: Boolean,
) {
    companion object {
        fun fill(data: String, role: GlyphRole) = GlyphPath(data, role, filled = true, 0f, false, false)

        fun stroke(data: String, role: GlyphRole, width: Float, roundCap: Boolean, roundJoin: Boolean) =
            GlyphPath(data, role, filled = false, width, roundCap, roundJoin)
    }
}

/**
 * One of the app's icons: its paths on a 24 × 24 grid, each in a colour of the theme.
 *
 * A tinted icon (the usual case) wears the tint and nothing else. One drawn untinted — the red
 * close mark — wears its own colours, which follow the theme as the Android drawables' night
 * resources did. The same type on Android and on the iPhone (DECISION-094).
 */
@Immutable
class DirectoryGlyph internal constructor(
    internal val paths: List<GlyphPath>,
    internal val mirrored: Boolean,
) {
    /** The icon as a vector in the theme's colours. */
    @Composable
    fun vector(): ImageVector {
        val dark = LocalDirectoryDark.current
        return remember(this, dark) { build(dark) }
    }

    internal fun build(dark: Boolean): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = mirrored,
        )
        paths.forEach { path ->
            val colour = SolidColor(path.role.colour(dark))
            builder.addPath(
                pathData = addPathNodes(path.data),
                fill = if (path.filled) colour else null,
                stroke = if (path.filled) null else colour,
                strokeLineWidth = path.width,
                strokeLineCap = if (path.roundCap) StrokeCap.Round else StrokeCap.Butt,
                strokeLineJoin = if (path.roundJoin) StrokeJoin.Round else StrokeJoin.Miter,
            )
        }
        return builder.build()
    }
}

internal fun GlyphRole.colour(dark: Boolean): Color = hexColor(
    when (this) {
        GlyphRole.CONTENT ->
            if (dark) DirectoryTokens.SemanticDarkContentPrimary else DirectoryTokens.SemanticContentPrimary
        GlyphRole.DANGER ->
            if (dark) DirectoryTokens.SemanticDarkFeedbackDanger else DirectoryTokens.SemanticFeedbackDanger
        GlyphRole.ON_PRIMARY ->
            if (dark) DirectoryTokens.SemanticDarkContentOnPrimary else DirectoryTokens.SemanticContentOnPrimary
    },
)

/** An icon of the shared set: a 1.8 stroke with round ends and corners, as the site draws it. */
private fun shared(icon: IconPaths) = DirectoryGlyph(
    icon.paths.map { GlyphPath.stroke(it, GlyphRole.CONTENT, width = 1.8f, roundCap = true, roundJoin = true) },
    mirrored = icon.mirrored,
)

/**
 * The app's icons: one line weight, one corner treatment, one place to change them.
 *
 * Wherever the shared set covers a concept (`packages/design-tokens/icons`, generated as
 * `dl_ic_*`), the app draws that icon, so a phone, a pin or a bell looks the same in the app, on
 * the site and in the console. The few this module still draws itself — the travel modes, a
 * filled star, a camera — are concepts the shared set does not have yet. Directional icons mirror
 * themselves for Arabic.
 */
object DirectoryIcons {
    // From the shared set, with no module-drawn equivalent before.
    val whatsapp = shared(DirectoryIconPaths.whatsapp)
    val heart = shared(DirectoryIconPaths.heart)
    val share = shared(DirectoryIconPaths.share)
    val calendar = shared(DirectoryIconPaths.calendar)
    val flag = shared(DirectoryIconPaths.flag)
    val emergency = shared(DirectoryIconPaths.emergency)
    val trash = shared(DirectoryIconPaths.trash)
    val settings = shared(DirectoryIconPaths.settings)
    val offline = shared(DirectoryIconPaths.wifiOff)
    val lock = shared(DirectoryIconPaths.lock)
    val history = shared(DirectoryIconPaths.history)
    val externalLink = shared(DirectoryIconPaths.externalLink)
    val checkCircle = shared(DirectoryIconPaths.checkCircle)
    val chart = shared(DirectoryIconPaths.chart)
    val moon = shared(DirectoryIconPaths.moon)
    val sun = shared(DirectoryIconPaths.sun)


    val search = shared(DirectoryIconPaths.search)
    val pin = shared(DirectoryIconPaths.mapPin)
    val star = shared(DirectoryIconPaths.star)
    val starFilled = ModuleGlyphs.starFilled
    val clock = shared(DirectoryIconPaths.clock)
    val phone = shared(DirectoryIconPaths.phone)
    val check = shared(DirectoryIconPaths.check)
    val close = shared(DirectoryIconPaths.close)
    val closeBox = ModuleGlyphs.closeBox
    val chevron = shared(DirectoryIconPaths.chevron)
    val back = shared(DirectoryIconPaths.arrowBack)
    val refresh = shared(DirectoryIconPaths.refresh)
    val warning = shared(DirectoryIconPaths.alert)
    val info = shared(DirectoryIconPaths.info)
    val person = shared(DirectoryIconPaths.user)
    val home = shared(DirectoryIconPaths.home)
    val map = shared(DirectoryIconPaths.map)
    val myLocation = ModuleGlyphs.myLocation
    val bell = shared(DirectoryIconPaths.bell)
    val camera = ModuleGlyphs.camera
    val upload = shared(DirectoryIconPaths.upload)
    val edit = shared(DirectoryIconPaths.edit)
    val plus = shared(DirectoryIconPaths.plus)
    val minus = ModuleGlyphs.minus
    val filter = shared(DirectoryIconPaths.filter)
    val route = shared(DirectoryIconPaths.directions)

    /** The three ways someone gets to a facility, told apart at a glance on the route screen. */
    val walk = ModuleGlyphs.walk
    val motorcycle = ModuleGlyphs.motorcycle
    val car = ModuleGlyphs.car
    val verified = shared(DirectoryIconPaths.verified)

    /** Whether a password is being shown; the mark is the control, not a sentence. */
    val eye = shared(DirectoryIconPaths.eye)
    val eyeOff = ModuleGlyphs.eyeOff
    val image = shared(DirectoryIconPaths.image)
    val hospital = ModuleGlyphs.hospital
    val pharmacy = shared(DirectoryIconPaths.pharmacy)
    val clinic = shared(DirectoryIconPaths.clinic)
    val laboratory = shared(DirectoryIconPaths.lab)
    val nursing = ModuleGlyphs.nursing
    val supplies = ModuleGlyphs.supplies
    val grid = shared(DirectoryIconPaths.grid)
    val logout = shared(DirectoryIconPaths.logout)
    val document = ModuleGlyphs.document
    val schedule = ModuleGlyphs.timeSlot

    /**
     * The icon for a category, by the key the backend gives it. An unknown key falls back to the
     * app's own mark rather than to nothing, and the category's name is always written beside it.
     */
    /**
     * The mark a category wears, by the key the backend gives it.
     *
     * Every category the platform serves has a shape of its own. A rail where two sections
     * share an icon — or fall back to the same generic grid — makes the reader read the labels
     * one by one, which is the work the icons were there to save.
     */
    fun category(iconKey: String?): DirectoryGlyph = when (iconKey) {
        "pharmacy" -> pharmacy
        "hospital" -> hospital
        "clinic", "medical-clinic" -> clinic
        "laboratory", "medical-laboratory" -> laboratory
        "nursing", "nursing-center" -> nursing
        "supplies", "medical-supplies" -> supplies
        else -> grid
    }
}

/** An icon. [contentDescription] is null when the words beside it already say what it means. */
@Composable
fun DirectoryIcon(
    icon: DirectoryGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.medium,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = icon.vector(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
