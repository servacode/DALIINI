package com.servacode.directory.core.inject

// The harness is plain Kotlin/JVM and cannot compile `:core:inject`'s expect declarations. On
// the JVM the bridge is what Android makes it: javax.inject's own annotations (DECISION-087).
typealias Inject = javax.inject.Inject

typealias Singleton = javax.inject.Singleton
