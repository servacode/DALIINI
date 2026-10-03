package com.servacode.directory.core.auth

interface RefreshTokenVault {
    fun read(): String?
    fun write(value: String)
    fun clear()
}
