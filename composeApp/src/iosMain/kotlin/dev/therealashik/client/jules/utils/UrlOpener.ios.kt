package dev.therealashik.client.jules.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    val scheme = nsUrl.scheme
    if (scheme != "http" && scheme != "https") {
        return
    }
    UIApplication.sharedApplication.openURL(nsUrl)
}
