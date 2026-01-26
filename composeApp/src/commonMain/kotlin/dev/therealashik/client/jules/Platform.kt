package dev.therealashik.client.jules

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
