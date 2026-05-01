package com.vxncius.authx.ui
import android.app.Activity
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? Activity
    SideEffect {
        activity?.window?.statusBarColor = Color.White.toArgb()
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        }
    }
    Scaffold(
        containerColor = Color.White
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    loadUrl(url)
                }
            },
            update = { webView ->
                webView.loadUrl(url)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

