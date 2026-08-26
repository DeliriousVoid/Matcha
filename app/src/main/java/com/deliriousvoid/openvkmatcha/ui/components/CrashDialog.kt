package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.graphics.Color
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.R

@Composable
fun CrashDialog(
    logs: String?,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    
    val funnyMessages = remember {
        listOf(
            R.string.crash_message_1,
            R.string.crash_message_2,
            R.string.crash_message_3,
            R.string.crash_message_4,
            R.string.crash_message_5,
            R.string.crash_message_6,
            R.string.crash_message_7
        )
    }
    
    val randomMessage = remember { funnyMessages.random() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            ThemedPopupBackground {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.mipmap.matcha)
                            .build()
                    ),
                    contentDescription = stringResource(id = R.string.app_icon),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.crash_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(id = randomMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        logs?.let {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.crash_copy_logs))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(stringResource(id = R.string.crash_close))
                }
            }
        }
    }
}
}
