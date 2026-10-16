package com.servacode.directory.core.designsystem

import androidx.annotation.DrawableRes

/**
 * A category's mark as an Android drawable, for what draws Android drawables rather than Compose:
 * the map's pins, which MapLibre paints into bitmaps. The same marks as [DirectoryIcons.category]:
 * the shared set's from the token package (`dl_ic_*`), and this module's own three kept as vector
 * drawables beside their Kotlin paths (CategoryDrawablesTest holds the two to each other).
 */
object CategoryDrawables {
    @DrawableRes
    fun of(iconKey: String?): Int = when (iconKey) {
        "pharmacy" -> R.drawable.dl_ic_pharmacy
        "hospital" -> R.drawable.ic_hospital
        "clinic", "medical-clinic" -> R.drawable.dl_ic_clinic
        "laboratory", "medical-laboratory" -> R.drawable.dl_ic_lab
        "nursing", "nursing-center" -> R.drawable.ic_nursing
        "supplies", "medical-supplies" -> R.drawable.ic_supplies
        "bakery" -> R.drawable.dl_ic_bakery
        "cart", "market" -> R.drawable.dl_ic_cart
        "restaurant" -> R.drawable.dl_ic_restaurant
        "fuel", "fuel-station" -> R.drawable.dl_ic_fuel
        "cash", "exchange" -> R.drawable.dl_ic_cash
        "tool", "workshop" -> R.drawable.dl_ic_tool
        else -> R.drawable.dl_ic_grid
    }
}
