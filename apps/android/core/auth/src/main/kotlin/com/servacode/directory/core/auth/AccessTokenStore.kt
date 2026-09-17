package com.servacode.directory.core.auth

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface AccessTokenStore {
    fun get(): String?
    fun set(value: String?)
}

@Singleton
class MemoryAccessTokenStore @Inject constructor() : AccessTokenStore {
    private val token = AtomicReference<String?>(null)
    override fun get(): String? = token.get()
    override fun set(value: String?) { token.set(value) }
}
