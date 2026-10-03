package com.servacode.directory.core.auth

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlin.concurrent.Volatile

interface AccessTokenStore {
    fun get(): String?
    fun set(value: String?)
}

@Singleton
class MemoryAccessTokenStore @Inject constructor() : AccessTokenStore {
    // Only read and replaced whole, never compared-and-set, so a volatile field is enough.
    @Volatile private var token: String? = null
    override fun get(): String? = token
    override fun set(value: String?) { token = value }
}
