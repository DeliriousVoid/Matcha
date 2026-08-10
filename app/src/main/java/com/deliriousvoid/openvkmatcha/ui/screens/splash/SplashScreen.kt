package com.deliriousvoid.openvkmatcha.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val tokenManager = OpenVKMatchaApp.instance.tokenManager

    LaunchedEffect(Unit) {
        if (tokenManager.hasToken()) {
            onNavigateToMain()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
