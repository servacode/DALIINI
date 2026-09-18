package com.servacode.directory.core.network.api

import com.servacode.directory.api.infrastructure.ApiClient
import com.servacode.directory.api.infrastructure.Serializer
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Part

/**
 * The generated P10 client, configured once for the whole app.
 *
 * Nothing outside this package touches `com.servacode.directory.api`: repositories see the
 * domain-facing boundaries, and the adapters here are the only callers of generated
 * operations.
 *
 * The OkHttp client is always the app's own. The generator's default builder installs a
 * body-level logging interceptor, which would write passwords and tokens to the log the
 * moment a logger was attached.
 */
class GeneratedClient(private val environment: ApiEnvironment, private val httpClient: OkHttpClient) {
    // Built on first use rather than at injection, so a build without a configured address
    // fails each request with a typed error instead of crashing the app at start-up.
    private val apiClient: ApiClient by lazy {
        configureSerialization()
        ApiClient(
            baseUrl = environment.requireConfiguredBaseUrl(),
            callFactory = httpClient,
            converterFactories = listOf(
                PlainTextPartConverterFactory,
                ScalarsConverterFactory.create(),
                Serializer.kotlinxSerializationJson.asConverterFactory("application/json".toMediaType()),
            ),
        )
    }

    fun <S> create(service: Class<S>): S = apiClient.createService(service)

    inline fun <reified S> create(): S = create(S::class.java)

    private companion object {
        private val configured = AtomicBoolean(false)

        /**
         * Adjusts the generated serializer before anything uses it; the generator only
         * accepts changes until first use.
         *
         * `encodeDefaults = false` and `explicitNulls = false`: generated request classes
         * default every optional field to null, and the generator's own setting sends those
         * nulls. A PATCH naming one field would then send every other field as null, which
         * the backend either refuses or applies as "clear this field".
         *
         * The contextual `Map` serializer: free-form JSON objects are generated as
         * `@Contextual Map<String, JsonElement>`, and a contextual interface has no default
         * serializer to fall back on.
         */
        fun configureSerialization() {
            if (!configured.compareAndSet(false, true)) return
            Serializer.kotlinxSerializationJsonConfiguration = {
                encodeDefaults = false
                explicitNulls = false
            }
            Serializer.kotlinxSerializationAdaptersConfiguration = {
                @Suppress("UNCHECKED_CAST")
                contextual(
                    Map::class as KClass<Map<String, JsonElement>>,
                    MapSerializer(String.serializer(), JsonElement.serializer()),
                )
            }
        }
    }
}

/**
 * Sends a UUID multipart field as its plain text.
 *
 * The generated evidence upload declares `@Part("requirementId") requirementId: UUID`.
 * Without this factory Retrofit hands the UUID to the JSON converter, which writes it with
 * its quotes, and the backend rejects `"…"` as a UUID.
 */
internal object PlainTextPartConverterFactory : Converter.Factory() {
    private val textPlain = "text/plain; charset=utf-8".toMediaType()

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        if (type != UUID::class.java || parameterAnnotations.none { it is Part }) return null
        return Converter<UUID, RequestBody> { value -> value.toString().toRequestBody(textPlain) }
    }
}
