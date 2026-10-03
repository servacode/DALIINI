package com.servacode.directory.feature.home

import androidx.compose.runtime.Composable
import com.servacode.directory.core.designsystem.DirectoryVocabulary
import com.servacode.directory.core.designsystem.StatusText
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.DirectoryBrand
import org.jetbrains.compose.resources.stringResource

/**
 * Everything Home says, read from the module's own resources.
 *
 * The words themselves live in `composeResources/values/strings.xml`, which is what makes a
 * second language a second file rather than a search through every screen: each platform picks
 * the file that matches the phone's language, and nothing here changes. This object stays because the screens read
 * their words through it, and because a name like `HomeCopy.SEARCH` says what the string is for
 * in a way `Res.string.home_search` does not.
 *
 * Each member is read in composition, which is why they are `@Composable`. Anything that needs
 * a word outside composition — a view model, a notification — has to be handed it rather than
 * reaching for it, and that is deliberate: a string that depends on the reader's language is
 * not a constant.
 */
object HomeCopy {
    /** The app names itself in one place; this is that place read back. */
    const val TITLE = DirectoryBrand.NAME

    val YOU_ARE_IN: String @Composable get() = stringResource(Res.string.home_you_are_in)
    val NOTIFICATIONS: String @Composable get() = stringResource(Res.string.home_notifications)
    val MANY: String @Composable get() = stringResource(Res.string.home_many)
    val EMERGENCY: String @Composable get() = stringResource(Res.string.home_emergency)
    val EMERGENCY_HINT: String
        @Composable get() = stringResource(Res.string.home_emergency_hint)
    val RECENT: String @Composable get() = stringResource(Res.string.home_recent)
    val DATA_SAVER_TITLE: String
        @Composable get() = stringResource(Res.string.home_data_saver_title)
    val DATA_SAVER_BODY: String
        @Composable get() = stringResource(Res.string.home_data_saver_body)
    val DATA_SAVER_ACCEPT: String
        @Composable get() = stringResource(Res.string.home_data_saver_accept)
    val DATA_SAVER_DISMISS: String
        @Composable get() = stringResource(Res.string.home_data_saver_dismiss)

    /** The bell as a screen reader says it when something is waiting. */
    @Composable
    fun unreadNotifications(count: Int): String = stringResource(Res.string.home_notifications_unread, count)
    val SEARCH: String @Composable get() = stringResource(Res.string.home_search)
    val FILTERS: String @Composable get() = stringResource(Res.string.home_filters)
    val LOADING_MORE: String @Composable get() = stringResource(Res.string.home_loading_more)

    /** The rows of a category's specialties and services, and the chips that undo them. */
    val SPECIALTIES: String @Composable get() = stringResource(Res.string.home_specialties)
    val SERVICES: String @Composable get() = stringResource(Res.string.home_services)
    val ALL_SPECIALTIES: String
        @Composable get() = stringResource(Res.string.home_all_specialties)
    val ALL_SERVICES: String @Composable get() = stringResource(Res.string.home_all_services)
    val CLEAR_TAGS: String @Composable get() = stringResource(Res.string.home_clear_tags)
    val EMPTY_CHOICE_BODY: String
        @Composable get() = stringResource(Res.string.home_empty_choice_body)

    val ERROR: String @Composable get() = stringResource(Res.string.home_error)
    val PROVINCE_REQUIRED: String
        @Composable get() = stringResource(Res.string.home_province_required)
    val PROVINCE_REQUIRED_BODY: String
        @Composable get() = stringResource(Res.string.home_province_required_body)
    val PROVINCE_CHOOSE: String
        @Composable get() = stringResource(Res.string.home_province_choose)

    val LOCATION_TITLE: String
        @Composable get() = stringResource(Res.string.home_location_title)
    val LOCATION_BODY: String
        @Composable get() = stringResource(Res.string.home_location_body)
    val LOCATION_ACTION: String
        @Composable get() = stringResource(Res.string.home_location_action)

    @Composable
    fun chip(chip: HomeChip): String = when (chip) {
        HomeChip.NEAREST -> stringResource(Res.string.home_chip_nearest)
        HomeChip.OPEN_NOW -> DirectoryVocabulary.availability(AvailabilityState.OPEN)
        HomeChip.DUTY_TODAY -> StatusText.ON_DUTY_TODAY
    }

    /**
     * Why the list is empty, in terms of what was asked.
     *
     * "No results" would leave the reader to work out which of their choices produced none, and
     * the combinations are exactly where that is hardest to guess.
     */
    @Composable
    fun emptyFor(filters: HomeFilters): String = stringResource(
        when (filters.emptyReason()) {
            HomeEmptyReason.CHOICE -> Res.string.home_empty_choice
            HomeEmptyReason.DUTY_AND_OPEN -> Res.string.home_empty_duty_and_open
            HomeEmptyReason.DUTY -> Res.string.home_empty_duty
            HomeEmptyReason.OPEN -> Res.string.home_empty_open
            HomeEmptyReason.CATEGORY -> Res.string.home_empty_category
        },
    )

    /**
     * And where that is, when the platform knows.
     *
     * A page that says only "no results" leaves the reader wondering whether the app is broken,
     * whether they are in the wrong place, or whether there is genuinely nothing. Naming the
     * place answers all three: nothing has been added here yet, and here is a real place.
     */
    @Composable
    fun emptyBodyFor(place: String?): String = when (place) {
        null -> stringResource(Res.string.home_empty_body)
        else -> stringResource(Res.string.home_empty_body_here, place)
    }
}
