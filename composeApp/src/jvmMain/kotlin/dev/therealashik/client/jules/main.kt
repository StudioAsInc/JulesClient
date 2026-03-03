package dev.therealashik.client.jules

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import dev.therealashik.client.jules.navigation.DeepLinkHandler

fun main(args: Array<String>) = application {
    val initialUrl = args.firstOrNull { it.startsWith("jules://") }
    if (initialUrl != null) {
        DeepLinkHandler.handleDeepLink(initialUrl)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Jules Client",
    ) {
        App()
    }
}