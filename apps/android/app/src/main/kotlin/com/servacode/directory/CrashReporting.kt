package com.servacode.directory

import android.app.Application
import com.servacode.directory.core.observability.NoOpObservability
import com.servacode.directory.core.observability.Observability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import javax.inject.Singleton

/**
 * Crash reporting, on only when the build carries a DSN (`DIRECTORY_SENTRY_DSN`). Without one the
 * SDK is never initialised — its manifest providers are removed, so not even its start-up code
 * runs — and nothing leaves the device.
 *
 * With one, what is sent is the crash itself: the stack, the device model and Android version,
 * the release (`versionName+versionCode`) and the flavour. No personal data: Sentry's default PII
 * is off, no screenshot or view hierarchy is attached, sessions are not tracked (they would
 * carry an installation id), and every event loses its user, its request and the data of its
 * breadcrumbs before it is sent. Deobfuscating a release's stack needs that release's R8
 * mapping (`app/build/outputs/mapping/<variant>/mapping.txt`) uploaded to Sentry.
 */
object CrashReporting {
    val enabled: Boolean get() = BuildConfig.SENTRY_DSN.isNotBlank()

    fun start(application: Application) {
        if (!enabled) return
        SentryAndroid.init(application) { options ->
            options.dsn = BuildConfig.SENTRY_DSN.trim()
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.dist = BuildConfig.VERSION_CODE.toString()
            options.environment = BuildConfig.FLAVOR
            options.setTag("versionCode", BuildConfig.VERSION_CODE.toString())
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isEnableAutoSessionTracking = false
            options.isEnableUserInteractionBreadcrumbs = false
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> scrub(event) }
            options.beforeBreadcrumb = SentryOptions.BeforeBreadcrumbCallback { breadcrumb, _ ->
                breadcrumb.also { it.data.clear() }
            }
        }
    }

    private fun scrub(event: SentryEvent): SentryEvent = event.apply {
        user = null
        request = null
        serverName = null
        breadcrumbs?.forEach { it.data.clear() }
    }
}

/**
 * The network layer's errors as breadcrumbs on the next crash — the kind and the backend's
 * request id, which is what an operator looks up; never a URL, a body or a token.
 */
object SentryObservability : Observability {
    override fun recordRequest(requestId: String, route: String, statusCode: Int, durationMs: Long) = Unit

    override fun recordError(kind: String, requestId: String?) {
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                category = "app.error"
                level = SentryLevel.WARNING
                message = listOfNotNull(kind, requestId?.let { "requestId=$it" }).joinToString(" ")
            },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ObservabilityModule {
    @Provides @Singleton
    fun provideObservability(): Observability =
        if (CrashReporting.enabled) SentryObservability else NoOpObservability
}
