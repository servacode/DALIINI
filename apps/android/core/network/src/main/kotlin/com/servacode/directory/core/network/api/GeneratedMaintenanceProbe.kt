package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.PublicTaxonomyApi
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.MaintenanceEnvelope
import com.servacode.directory.core.network.MaintenanceProbe
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.MaintenanceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Asks the backend whether maintenance is over.
 *
 * First the platform status (`GET api/v1/platform/status/`, `{maintenance, messageAr,
 * retryAfterSeconds}`), which is made for exactly this question and is answered during
 * maintenance. The generated client does not carry it yet, so it is read here with the app's own
 * anonymous OkHttp client. A backend that does not serve it yet — 404, 405, a body that is not
 * the status — falls back to the served provinces: the smallest public request the app already
 * makes, and one that maintenance does not exempt, so its 503 or 200 answers the same question.
 */
class GeneratedMaintenanceProbe(
    private val environment: ApiEnvironment,
    private val http: OkHttpClient,
    anonymous: GeneratedClient,
    private val state: MaintenanceState,
) : MaintenanceProbe {
    private val taxonomy by lazy { anonymous.create<PublicTaxonomyApi>() }

    override suspend fun probe() {
        when (val status = platformStatus()) {
            PlatformStatus.Unavailable -> call { taxonomy.publicProvincesList() }
            PlatformStatus.Normal -> state.clear()
            is PlatformStatus.Maintenance -> {
                state.enter(status.active)
                throw AppException(AppError(AppError.Kind.SERVER, code = MaintenanceEnvelope.CODE, status = 503))
            }
        }
    }

    private suspend fun platformStatus(): PlatformStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(environment.requireConfiguredBaseUrl() + STATUS_PATH)
            .get()
            .build()
        // Offline and every other transport failure surface as they would from any request.
        http.newCall(request).execute().use { response ->
            when {
                // The interceptor has already recorded a maintenance envelope.
                response.code == 503 -> throw AppException(
                    AppError(AppError.Kind.SERVER, code = MaintenanceEnvelope.CODE, status = 503),
                )
                response.isSuccessful -> parseStatus(response.body.string())
                else -> PlatformStatus.Unavailable
            }
        }
    }

    internal sealed interface PlatformStatus {
        data object Normal : PlatformStatus
        data class Maintenance(val active: MaintenanceStatus.Active) : PlatformStatus

        /** The endpoint is not there, or did not answer with the status. */
        data object Unavailable : PlatformStatus
    }

    companion object {
        /** Relative to the API base URL; the interceptor leaves its answers to this probe. */
        const val STATUS_PATH = "api/v1/platform/status/"

        private val json = Json { ignoreUnknownKeys = true }

        internal fun parseStatus(body: String): PlatformStatus {
            val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
                ?: return PlatformStatus.Unavailable
            val maintenance = (root["maintenance"] as? JsonPrimitive)?.booleanOrNull
                ?: return PlatformStatus.Unavailable
            if (!maintenance) return PlatformStatus.Normal
            return PlatformStatus.Maintenance(
                MaintenanceStatus.Active(
                    message = (root["messageAr"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() },
                    retryAfterSeconds = (root["retryAfterSeconds"] as? JsonPrimitive)?.longOrNull
                        ?.takeIf { it >= 0 },
                ),
            )
        }
    }
}
