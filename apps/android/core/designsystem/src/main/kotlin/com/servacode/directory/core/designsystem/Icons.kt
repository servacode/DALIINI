package com.servacode.directory.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/**
 * The app's own icons: one line weight, one corner treatment, one place to change them.
 *
 * They are drawn in this module rather than taken from an icon library, so the set stays small,
 * matches the brand's line, and adds no dependency. Directional ones mirror themselves for Arabic.
 */
object DirectoryIcons {
    @DrawableRes val search = R.drawable.ic_search
    @DrawableRes val pin = R.drawable.ic_pin
    @DrawableRes val star = R.drawable.ic_star
    @DrawableRes val clock = R.drawable.ic_clock
    @DrawableRes val phone = R.drawable.ic_phone
    @DrawableRes val check = R.drawable.ic_check
    @DrawableRes val chat = R.drawable.ic_chat
    @DrawableRes val close = R.drawable.ic_close
    @DrawableRes val chevron = R.drawable.ic_chevron
    @DrawableRes val back = R.drawable.ic_arrow_back
    @DrawableRes val refresh = R.drawable.ic_refresh
    @DrawableRes val warning = R.drawable.ic_warning
    @DrawableRes val info = R.drawable.ic_info
    @DrawableRes val person = R.drawable.ic_person
    @DrawableRes val home = R.drawable.ic_home
    @DrawableRes val map = R.drawable.ic_map
    @DrawableRes val bell = R.drawable.ic_bell
    @DrawableRes val camera = R.drawable.ic_camera
    @DrawableRes val upload = R.drawable.ic_upload
    @DrawableRes val edit = R.drawable.ic_edit
    @DrawableRes val plus = R.drawable.ic_plus
    @DrawableRes val minus = R.drawable.ic_minus
    @DrawableRes val filter = R.drawable.ic_filter
    @DrawableRes val route = R.drawable.ic_route

    /** The three ways someone gets to a facility, told apart at a glance on the route screen. */
    @DrawableRes val walk = R.drawable.ic_walk
    @DrawableRes val motorcycle = R.drawable.ic_motorcycle
    @DrawableRes val car = R.drawable.ic_car
    @DrawableRes val verified = R.drawable.ic_shield_check

    /** Whether a password is being shown; the mark is the control, not a sentence. */
    @DrawableRes val eye = R.drawable.ic_eye
    @DrawableRes val eyeOff = R.drawable.ic_eye_off
    @DrawableRes val image = R.drawable.ic_image
    @DrawableRes val hospital = R.drawable.ic_hospital
    @DrawableRes val pharmacy = R.drawable.ic_pharmacy
    @DrawableRes val clinic = R.drawable.ic_clinic
    @DrawableRes val laboratory = R.drawable.ic_laboratory
    @DrawableRes val nursing = R.drawable.ic_nursing
    @DrawableRes val supplies = R.drawable.ic_supplies
    @DrawableRes val grid = R.drawable.ic_grid
    @DrawableRes val logout = R.drawable.ic_logout
    @DrawableRes val document = R.drawable.ic_document
    @DrawableRes val schedule = R.drawable.ic_time_slot

    /**
     * The icon for a category, by the key the backend gives it. An unknown key falls back to the
     * app's own mark rather than to nothing, and the category's name is always written beside it.
     */
    @DrawableRes
    /**
     * The mark a category wears, by the key the backend gives it.
     *
     * Every category the platform serves has a shape of its own. A rail where two sections
     * share an icon — or fall back to the same generic grid — makes the reader read the labels
     * one by one, which is the work the icons were there to save.
     */
    fun category(iconKey: String?): Int = when (iconKey) {
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
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.medium,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
