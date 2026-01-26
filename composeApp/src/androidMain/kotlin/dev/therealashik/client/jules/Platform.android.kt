package dev.therealashik.client.jules

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as AndroidBackHandler

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    AndroidBackHandler(enabled, onBack)
}
