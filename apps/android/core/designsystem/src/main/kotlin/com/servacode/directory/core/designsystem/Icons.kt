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
    @DrawableRes val whatsapp = R.drawable.dl_ic_whatsapp
    @DrawableRes val heart = R.drawable.dl_ic_heart
    @DrawableRes val share = R.drawable.dl_ic_share
    @DrawableRes val calendar = R.drawable.dl_ic_calendar
    @DrawableRes val flag = R.drawable.dl_ic_flag
    @DrawableRes val emergency = R.drawable.dl_ic_emergency
    @DrawableRes val trash = R.drawable.dl_ic_trash
    @DrawableRes val settings = R.drawable.dl_ic_settings
    @DrawableRes val offline = R.drawable.dl_ic_wifi_off
    @DrawableRes val lock = R.drawable.dl_ic_lock
    @DrawableRes val history = R.drawable.dl_ic_history
    @DrawableRes val externalLink = R.drawable.dl_ic_external_link
    @DrawableRes val checkCircle = R.drawable.dl_ic_check_circle
    @DrawableRes val chart = R.drawable.dl_ic_chart
    @DrawableRes val moon = R.drawable.dl_ic_moon
    @DrawableRes val sun = R.drawable.dl_ic_sun


    @DrawableRes val search = R.drawable.dl_ic_search
    @DrawableRes val pin = R.drawable.dl_ic_map_pin
    @DrawableRes val star = R.drawable.dl_ic_star
    @DrawableRes val starFilled = R.drawable.ic_star_filled
    @DrawableRes val clock = R.drawable.dl_ic_clock
    @DrawableRes val phone = R.drawable.dl_ic_phone
    @DrawableRes val check = R.drawable.dl_ic_check
    @DrawableRes val close = R.drawable.dl_ic_close
    @DrawableRes val closeBox = R.drawable.ic_close_box
    @DrawableRes val chevron = R.drawable.dl_ic_chevron
    @DrawableRes val back = R.drawable.dl_ic_arrow_back
    @DrawableRes val refresh = R.drawable.dl_ic_refresh
    @DrawableRes val warning = R.drawable.dl_ic_alert
    @DrawableRes val info = R.drawable.dl_ic_info
    @DrawableRes val person = R.drawable.dl_ic_user
    @DrawableRes val home = R.drawable.dl_ic_home
    @DrawableRes val map = R.drawable.dl_ic_map
    @DrawableRes val myLocation = R.drawable.ic_my_location
    @DrawableRes val bell = R.drawable.dl_ic_bell
    @DrawableRes val camera = R.drawable.ic_camera
    @DrawableRes val upload = R.drawable.dl_ic_upload
    @DrawableRes val edit = R.drawable.dl_ic_edit
    @DrawableRes val plus = R.drawable.dl_ic_plus
    @DrawableRes val minus = R.drawable.ic_minus
    @DrawableRes val filter = R.drawable.dl_ic_filter
    @DrawableRes val route = R.drawable.dl_ic_directions

    /** The three ways someone gets to a facility, told apart at a glance on the route screen. */
    @DrawableRes val walk = R.drawable.ic_walk
    @DrawableRes val motorcycle = R.drawable.ic_motorcycle
    @DrawableRes val car = R.drawable.ic_car
    @DrawableRes val verified = R.drawable.dl_ic_verified

    /** Whether a password is being shown; the mark is the control, not a sentence. */
    @DrawableRes val eye = R.drawable.dl_ic_eye
    @DrawableRes val eyeOff = R.drawable.ic_eye_off
    @DrawableRes val image = R.drawable.dl_ic_image
    @DrawableRes val hospital = R.drawable.ic_hospital
    @DrawableRes val pharmacy = R.drawable.dl_ic_pharmacy
    @DrawableRes val clinic = R.drawable.dl_ic_clinic
    @DrawableRes val laboratory = R.drawable.dl_ic_lab
    @DrawableRes val nursing = R.drawable.ic_nursing
    @DrawableRes val supplies = R.drawable.ic_supplies
    @DrawableRes val grid = R.drawable.dl_ic_grid
    @DrawableRes val logout = R.drawable.dl_ic_logout
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
