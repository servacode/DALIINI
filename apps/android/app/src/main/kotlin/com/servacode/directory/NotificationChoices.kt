package com.servacode.directory

import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.NotificationCategory

/**
 * Whether a push of [category] is shown, by the reader's choices in Settings. A kind that cannot
 * be turned off — null — always is: an unknown type is not a reason to hide what the platform
 * sent. The device's copy of the choices: the account's own when signed in (DECISION-077).
 */
fun NotificationPreferences.allows(category: NotificationCategory?): Boolean = when (category) {
    null -> true
    NotificationCategory.DUTY_REMINDER -> dutyReminders
    NotificationCategory.PROVINCE_NEWS -> provinceNews
    NotificationCategory.APPLICATION_STATUS -> applicationStatus
}
