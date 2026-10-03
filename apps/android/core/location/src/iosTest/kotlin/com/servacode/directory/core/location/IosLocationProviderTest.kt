package com.servacode.directory.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorDomain
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosLocationProviderTest {
    private fun location(accuracy: Double, at: Double = 1_700_000_000.5) = CLLocation(
        coordinate = CLLocationCoordinate2DMake(33.5138, 36.2765),
        altitude = 690.0,
        horizontalAccuracy = accuracy,
        verticalAccuracy = 10.0,
        timestamp = NSDate.dateWithTimeIntervalSince1970(at),
    )

    @Test
    fun `a position becomes a fix with its accuracy and its time in milliseconds`() {
        val fix = location(accuracy = 12.5).toFix(precise = true)

        assertEquals(
            LocationFix(33.5138, 36.2765, 12.5f, precise = true, capturedAtEpochMillis = 1_700_000_000_500),
            fix,
        )
    }

    @Test
    fun `an approximate permission is reported as not precise`() {
        assertEquals(false, location(accuracy = 3_000.0).toFix(precise = false)?.precise)
    }

    @Test
    fun `a position Core Location marks invalid is no fix`() {
        assertNull(location(accuracy = -1.0).toFix(precise = true))
    }

    @Test
    fun `only a granted permission reads the position`() {
        assertTrue(isAllowed(kCLAuthorizationStatusAuthorizedWhenInUse))
        assertTrue(isAllowed(kCLAuthorizationStatusAuthorizedAlways))
        // Not yet asked counts as not allowed: this provider never asks, the app's screen does.
        assertFalse(isAllowed(kCLAuthorizationStatusNotDetermined))
        assertFalse(isAllowed(kCLAuthorizationStatusDenied))
        assertFalse(isAllowed(kCLAuthorizationStatusRestricted))
    }

    @Test
    fun `only a denial ends a stream`() {
        assertTrue(NSError.errorWithDomain(kCLErrorDomain, kCLErrorDenied.toLong(), null).isDenial())
        assertFalse(NSError.errorWithDomain(kCLErrorDomain, kCLErrorLocationUnknown.toLong(), null).isDenial())
        assertFalse(NSError.errorWithDomain("NSURLErrorDomain", kCLErrorDenied.toLong(), null).isDenial())
    }

    @Test
    fun `positions closer together than the interval are dropped`() {
        val throttle = Throttle(intervalMillis = 1_000)

        val admitted = listOf(0L, 200, 999, 1_000, 1_500, 2_100).filter(throttle::admit)

        assertEquals(listOf(0L, 1_000, 2_100), admitted)
    }
}
