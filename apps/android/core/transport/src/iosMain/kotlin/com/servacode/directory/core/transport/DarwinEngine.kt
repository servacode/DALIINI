package com.servacode.directory.core.transport

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * The iPhone's HTTP engine: Ktor on the system's URL loading (NSURLSession), which honours the
 * phone's proxy, its certificates and App Transport Security. Its failures to connect arrive as
 * an IOException, which the transport reads as offline, as OkHttp's are on Android.
 */
fun darwinEngine(): HttpClientEngine = Darwin.create()
