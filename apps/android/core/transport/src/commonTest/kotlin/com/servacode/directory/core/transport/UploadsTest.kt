package com.servacode.directory.core.transport

import kotlin.test.Test
import kotlin.test.assertEquals

/** The part an upload sends, as OkHttp built it on Android. */
class UploadsTest {
    @Test fun `a media type is sent only when OkHttp would read it`() {
        // Each expected value is whether OkHttp's MediaType.parse reads the same text.
        val expected = mapOf(
            "image/jpeg" to true,
            "image/jpeg; charset=utf-8" to true,
            "text/plain;charset=\"utf-8\"" to true,
            "image" to false,
            "image/" to false,
            "image/jpeg x" to false,
            "" to false,
            "image/jpeg;" to true,
            "image/jpeg; foo" to false,
            "image/jpeg;foo=bar;" to true,
            "IMAGE/JPEG" to true,
            "image/jpeg ;a=b" to false,
            "image/jpeg;a='b'" to true,
            "not a type" to false,
            "application/octet-stream" to true,
            "a/b;c=\"d;e\"" to true,
            "a/b; c=d e" to false,
        )
        expected.forEach { (type, readable) -> assertEquals(readable, isOkHttpMediaType(type), type) }
    }

    @Test fun `a file name is quoted the way OkHttp quotes it`() {
        assertEquals("\"upload.jpg\"", quotedForMultipart("upload.jpg"))
        assertEquals("\"a%22b%0D%0Ac\"", quotedForMultipart("a\"b\r\nc"))
        assertEquals("\"صورة.jpg\"", quotedForMultipart("صورة.jpg"))
    }
}
