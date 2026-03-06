package dev.therealashik.client.jules.utils

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    val uri = URI(url)
    val scheme = uri.scheme
    if (scheme != "http" && scheme != "https") {
        return
    }

    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(uri)
    }
}
