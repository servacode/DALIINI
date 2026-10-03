package com.servacode.directory.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * A view model's test on every platform. A view model's scope runs on the main dispatcher, which
 * is a test one for the length of [body], as `MainDispatcherRule` makes it under JUnit; the
 * test's own scheduler is that dispatcher's, so `advanceUntilIdle()` reaches the view model.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runMainTest(body: suspend TestScope.() -> Unit): TestResult {
    val dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        return runTest(dispatcher, testBody = body)
    } finally {
        Dispatchers.resetMain()
    }
}
