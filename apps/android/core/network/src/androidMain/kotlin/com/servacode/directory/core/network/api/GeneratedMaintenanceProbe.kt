package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.PublicPlatformApi
import com.servacode.directory.api.models.PlatformStatus
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.MaintenanceEnvelope
import com.servacode.directory.core.network.MaintenanceProbe
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.MaintenanceStatus

/**
 * Asks the backend whether maintenance is over, through `GET api/v1/platform/status/`.
 *
 * The status endpoint is made for this question and answers it during maintenance, so its body
 * decides: `maintenance: false` ends the notice, `true` keeps it and refreshes its words and its
 * retry hint. A 503 maintenance envelope keeps it too (the interceptor has already read it).
 *
 * Any other answer from the backend — a 404 from a server that predates the endpoint, a 400 — is
 * still an answer, and a backend that answers is not in maintenance: the notice ends rather
 * than staying up forever. Only no answer at all (offline) or a server error leaves it as it is.
 */
class GeneratedMaintenanceProbe(
    anonymous: GeneratedClient,
    private val state: MaintenanceState,
) : MaintenanceProbe {
    private val platform by lazy { anonymous.create<PublicPlatformApi>() }

    override suspend fun probe() {
        val status = try {
            call { platform.publicPlatformStatusRetrieve() }
        } catch (failure: AppException) {
            val error = failure.error
            val stillDown = error.code == MaintenanceEnvelope.CODE ||
                error.kind == AppError.Kind.OFFLINE ||
                error.kind == AppError.Kind.SERVER ||
                error.kind == AppError.Kind.UNEXPECTED
            if (stillDown) throw failure
            state.clear()
            return
        }
        apply(status)
    }

    private fun apply(status: PlatformStatus) {
        val active = active(status)
        if (active == null) {
            state.clear()
            return
        }
        state.enter(active)
        throw AppException(AppError(AppError.Kind.SERVER, code = MaintenanceEnvelope.CODE, status = 503))
    }

    companion object {
        /** The notice [status] asks for, or null when the platform is up. */
        internal fun active(status: PlatformStatus): MaintenanceStatus.Active? =
            if (!status.maintenance) {
                null
            } else {
                MaintenanceStatus.Active(
                    message = status.messageAr.takeIf { it.isNotBlank() },
                    // Zero is "no hint", not "retry at once".
                    retryAfterSeconds = status.retryAfterSeconds.toLong().takeIf { it > 0 },
                )
            }
    }
}
