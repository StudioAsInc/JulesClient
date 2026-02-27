package dev.therealashik.client.jules.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlatformFile(
    private val uri: Uri,
    private val contentResolver: ContentResolver
) : PlatformFile {
    override val name: String
        get() {
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = cursor.getString(index)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    cursor?.close()
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != null && cut != -1) {
                    result = result?.substring(cut + 1)
                }
            }
            return result ?: "unknown"
        }

    override suspend fun readText(): String = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("Cannot read file")
    }
}

@Composable
actual fun rememberFilePickerLauncher(onFilePicked: (PlatformFile) -> Unit): FilePickerLauncher {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onFilePicked(AndroidPlatformFile(uri, contentResolver))
        }
    }
    return remember(launcher) {
        object : FilePickerLauncher {
            override fun launch() {
                launcher.launch("*/*")
            }
        }
    }
}
