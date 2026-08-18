package com.vxncius.authx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.vxncius.authx.R
import com.vxncius.authx.ui.theme.AuthXColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = composition != null,
        iterations = 1,
        restartOnPlay = true
    )
    var notified by remember { mutableStateOf(false) }
    val startedAt = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    val minDurationMs = 1500L

    fun notifyFinished() {
        if (notified) return
        notified = true
        val remaining = minDurationMs - (System.currentTimeMillis() - startedAt)
        if (remaining > 0) {
            scope.launch {
                delay(remaining)
                onFinished()
            }
        } else {
            onFinished()
        }
    }

    LaunchedEffect(composition) {
        if (composition == null) {
            delay(10_000)
            notifyFinished()
        }
    }
    LaunchedEffect(progress) {
        if (composition != null && progress >= 0.999f) {
            notifyFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthXColors.BgBase),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(0.3f),
            renderMode = RenderMode.HARDWARE
        )
    }
}