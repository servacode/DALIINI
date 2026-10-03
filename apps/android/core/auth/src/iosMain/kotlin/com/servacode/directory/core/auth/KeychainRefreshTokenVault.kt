package com.servacode.directory.core.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * The iPhone's [RefreshTokenVault]: the refresh secret as one generic-password item in the
 * Keychain, the place iOS gives an app for exactly this.
 *
 * As on Android, where the Keystore's key never leaves the phone:
 *  - **This device only.** The item is never restored to another phone from a backup, so a
 *    restored phone signs in again rather than carrying someone's session across.
 *  - **Readable after the first unlock.** A refresh can happen while the phone is locked, for
 *    instance when a notice arrives, as long as it was unlocked once since starting.
 *  - **Unreadable is signed out.** An item that cannot be read is removed, and the session ends.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainRefreshTokenVault(
    private val service: String = SERVICE,
) : RefreshTokenVault {

    override fun read(): String? = memScoped {
        val query = query(
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        when (status) {
            errSecSuccess -> {
                val data = CFBridgingRelease(result.value) as? NSData
                val text = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding) as String? }
                if (text.isNullOrEmpty()) {
                    clear()
                    null
                } else {
                    text
                }
            }
            else -> null
        }
    }

    override fun write(value: String) {
        // Replace rather than update: one call to remove whatever is there, one to add, so an
        // item written by an older build with other attributes cannot survive the change.
        clear()
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("the refresh secret is not text")
        val secret = CFBridgingRetain(data)
        val query = query(
            kSecValueData to secret,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        // The dictionary retained the secret's bytes; this hand lets go of its own reference.
        CFRelease(secret)
        val status = SecItemAdd(query, null)
        CFRelease(query)
        check(status == errSecSuccess) { "the Keychain refused the session ($status)" }
    }

    override fun clear() {
        val query = query()
        val status = SecItemDelete(query)
        CFRelease(query)
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "the Keychain refused to remove the session ($status)"
        }
    }

    /** The item's address (class, service, account) plus [extra]; the caller releases it. */
    private fun query(vararg extra: Pair<CFStringRef?, CFTypeRef?>): CFMutableDictionaryRef {
        val dictionary = requireNotNull(
            CFDictionaryCreateMutable(
                null,
                0,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            ),
        )
        val serviceName = CFBridgingRetain(service)
        val accountName = CFBridgingRetain(ACCOUNT)
        CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dictionary, kSecAttrService, serviceName)
        CFDictionaryAddValue(dictionary, kSecAttrAccount, accountName)
        extra.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        // The dictionary retained what it holds.
        CFRelease(serviceName)
        CFRelease(accountName)
        return dictionary
    }

    companion object {
        const val SERVICE = "com.servacode.directory.session"
        private const val ACCOUNT = "refresh"
    }
}
