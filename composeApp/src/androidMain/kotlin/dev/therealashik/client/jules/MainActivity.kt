package dev.therealashik.client.jules

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.therealashik.client.jules.navigation.DeepLinkHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AndroidContext.context = applicationContext

        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.data
        if (Intent.ACTION_VIEW == action && data != null) {
            DeepLinkHandler.handleDeepLink(data.toString())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}