package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.LegalDocument
import com.servacode.directory.api.models.LegalDocumentList

interface ContentApi {
    /**
     * GET api/v1/public/legal/{key}/
     * Retrieve one published page
     * One published page, in full.
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param key 
     * @return [LegalDocument]
     */
    @GET("api/v1/public/legal/{key}/")
    suspend fun publicLegalDocumentRetrieve(@Path("key") key: kotlin.String): Response<LegalDocument>

    /**
     * GET api/v1/public/legal/
     * List the published pages
     * Titles and versions only. A client compares the version it cached with the one here and fetches a page&#39;s words only when they have changed.
     * Responses:
     *  - 200: 
     *
     * @return [LegalDocumentList]
     */
    @GET("api/v1/public/legal/")
    suspend fun publicLegalDocumentsList(): Response<LegalDocumentList>

}
