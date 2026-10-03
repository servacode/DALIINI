package com.servacode.directory.ios

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * What a facility's page hands to the phone, as Android hands it to an intent: the dialer, a
 * WhatsApp link, the way there and the share sheet. A phone with nothing to take it does nothing.
 */
internal object IosActions {
    fun open(address: String) {
        val url = NSURL.URLWithString(address) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    /** The dialer with the number the backend published; the call is the reader's. */
    fun call(phone: String) = open("tel:" + phone.filter { it.isDigit() || it == '+' })

    /**
     * Apple Maps' way there. Android shows its own route first; the iPhone's comes with the map
     * (ROADMAP ٨), and until then the phone's own maps app gives the way.
     */
    fun directions(latitude: Double, longitude: Double) =
        open("https://maps.apple.com/?daddr=$latitude,$longitude")

    /** The share sheet, over [from], with [text]: the facility's name and its link. */
    fun share(from: UIViewController, text: String) {
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        from.presentViewController(sheet, animated = true, completion = null)
    }
}
