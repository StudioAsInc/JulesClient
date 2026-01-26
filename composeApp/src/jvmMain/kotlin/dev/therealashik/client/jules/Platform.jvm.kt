package dev.therealashik.client.jules

import androidx.compose.runtime.Composable

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on Desktop
}
