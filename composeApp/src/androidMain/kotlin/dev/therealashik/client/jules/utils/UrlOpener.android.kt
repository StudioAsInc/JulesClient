package dev.therealashik.client.jules.utils

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

actual fun openUrl(url: String) {
    val uri = Uri.parse(url)
    val scheme = uri.scheme
    if (scheme != "http" && scheme != "https") {
        return
    }

    val context = dev.therealashik.client.jules.AndroidContext.context
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ContextCompat.startActivity(context, intent, null)
}
