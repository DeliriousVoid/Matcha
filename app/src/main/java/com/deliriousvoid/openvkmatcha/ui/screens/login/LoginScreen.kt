package com.deliriousvoid.openvkmatcha.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.components.MatchaButton
import com.deliriousvoid.openvkmatcha.ui.components.MatchaTextField
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateTo2FA: (String, String, String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    
    LaunchedEffect(state.navigateTo2FA) {
        if (state.navigateTo2FA) {
            onNavigateTo2FA(state.username, state.password, state.instanceUrl)
            viewModel.onNavigatedTo2FA()
        }
    }
    val context = LocalContext.current
    val appIcon = remember(context) {
        context.packageManager.getApplicationIcon(context.packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Icon
            Image(
                painter = rememberAsyncImagePainter(model = appIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.union_logotype),
                contentDescription = "Matcha",
                modifier = Modifier.height(40.dp),
                contentScale = ContentScale.Fit
            )
            
            Text(
                text = matchaStringResource(R.string.welcome_to_openvk),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            MatchaTextField(
                value = state.instanceUrl,
                onValueChange = viewModel::updateInstance,
                label = matchaStringResource(R.string.instance_url),
                placeholder = "https://api.openvk.org"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = !state.useTokenLogin,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Column {
                    MatchaTextField(
                        value = state.username,
                        onValueChange = viewModel::updateUsername,
                        label = matchaStringResource(R.string.login_or_email)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MatchaTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = matchaStringResource(R.string.password),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }

            AnimatedVisibility(
                visible = state.useTokenLogin,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                MatchaTextField(
                    value = state.token,
                    onValueChange = viewModel::updateToken,
                    label = matchaStringResource(R.string.access_token)
                )
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(24.dp))
                ErrorText(
                    message = state.error!!,
                    onRetry = { viewModel.login(onLoginSuccess) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MatchaButton(
                text = if (state.useTokenLogin) matchaStringResource(R.string.login_by_token) else matchaStringResource(R.string.login),
                onClick = { viewModel.login(onLoginSuccess) },
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = viewModel::toggleLoginMode,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.useTokenLogin) matchaStringResource(R.string.login_by_credentials) else matchaStringResource(R.string.login_by_token),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = matchaStringResource(R.string.desc_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
