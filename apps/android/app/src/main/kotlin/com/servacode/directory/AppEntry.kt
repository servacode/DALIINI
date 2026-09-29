package com.servacode.directory

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.servacode.directory.core.model.DeepLinkTarget
import com.servacode.directory.core.model.DeepLinks
import com.servacode.directory.core.model.MessageDestination
import com.servacode.directory.core.model.NotificationTarget
import com.servacode.directory.core.network.PushMessageData

/** Where the app was asked to open: a link to the site, a notice that was tapped, the widget. */
sealed interface AppEntry {
    /** An https link to the site — an App Link, or the widget's, which uses the same paths. */
    data class Link(val target: DeepLinkTarget) : AppEntry

    /**
     * A push notice. It carries its type and at most a facility and a date, never its content;
     * a type with nowhere of its own to go opens the inbox, where the content is.
     */
    data class Notice(val type: String, val facilityId: String?, val date: String?) : AppEntry {
        val target: NotificationTarget
            get() = NotificationTarget.of(type, MessageDestination.NONE, facilityId, date)
    }
}

/**
 * Reading and writing the intents that carry an [AppEntry]. The activity is exported, so what an
 * intent says is checked like anything else from outside: a link must be to this build's site
 * and of a shape the app knows, and an identifier must look like one.
 */
object AppEntries {
    private const val EXTRA_NOTICE_TYPE = "com.servacode.directory.notice.TYPE"
    private const val EXTRA_NOTICE_FACILITY = "com.servacode.directory.notice.FACILITY_ID"
    private const val EXTRA_NOTICE_DATE = "com.servacode.directory.notice.DATE"
    private val IDENTIFIER = Regex("^[0-9A-Za-z-]{1,64}$")
    private val TYPE = Regex("^[a-z0-9._-]{1,64}$", RegexOption.IGNORE_CASE)

    /** The site's host for this build; `www.` is accepted by the parser as the same site. */
    val hosts: Set<String> get() = setOf(BuildConfig.APP_LINK_HOST.lowercase())

    fun from(intent: Intent?): AppEntry? = intent?.let {
        from(it.action, it.dataString, hosts) { key -> it.getStringExtra(key) }
    }

    fun from(action: String?, data: String?, hosts: Set<String>, extra: (String) -> String?): AppEntry? {
        if (action == Intent.ACTION_VIEW && data != null) {
            return DeepLinks.parse(data, hosts)?.let(AppEntry::Link)
        }
        val type = extra(EXTRA_NOTICE_TYPE)?.trim()?.takeIf { TYPE.matches(it) } ?: return null
        return AppEntry.Notice(
            type = type,
            facilityId = extra(EXTRA_NOTICE_FACILITY)?.trim()?.takeIf { IDENTIFIER.matches(it) },
            date = extra(EXTRA_NOTICE_DATE)?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    /** What a tapped push opens: this activity, brought forward rather than stacked again. */
    fun notice(context: Context, data: PushMessageData): Intent =
        Intent(context, MainActivity::class.java)
            .addFlags(FORWARD)
            .putExtra(EXTRA_NOTICE_TYPE, data.type)
            .putExtra(EXTRA_NOTICE_FACILITY, data.facilityId)
            .putExtra(EXTRA_NOTICE_DATE, data.date?.toString())

    /**
     * A link the app opens itself, from the widget: the site's own path, delivered straight to
     * this activity. Each path is its own intent, so two rows never share one tap.
     */
    fun link(context: Context, path: String): Intent =
        Intent(Intent.ACTION_VIEW, "https://${BuildConfig.APP_LINK_HOST}$path".toUri(), context, MainActivity::class.java)
            .addFlags(FORWARD)

    private const val FORWARD =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}
