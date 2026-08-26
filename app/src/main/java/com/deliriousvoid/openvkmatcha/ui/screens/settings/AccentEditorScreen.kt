package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.ui.components.ColorPicker
import com.deliriousvoid.openvkmatcha.ui.theme.CustomAccent
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentEditorScreen(
    accentId: String? = null,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val existingAccent = remember { viewModel.customAccents.value.find { it.id == accentId } }
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    
    var name by remember { mutableStateOf(existingAccent?.name ?: "") }
    var author by remember { mutableStateOf(existingAccent?.author ?: "") }
    
    val defaultPrimary = MaterialTheme.colorScheme.primary
    var primary by remember { mutableStateOf(existingAccent?.let { Color(it.primary) } ?: defaultPrimary) }
    
    var pickingColor by remember { mutableStateOf(false) }

    val titleNew = com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.title_new_accent)
    val titleEdit = com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.title_edit_accent)

    androidx.compose.runtime.DisposableEffect(accentId) {
        com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(com.deliriousvoid.openvkmatcha.util.TopBarState(
            title = if (accentId == null) titleNew else titleEdit,
            route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.SETTINGS_ACCENT_EDITOR,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        ))
        onDispose {
            com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Colors", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickingColor = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Primary Color")
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(primary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val id = accentId ?: "accent_${System.currentTimeMillis()}"
                    val accent = CustomAccent(
                        id = id,
                        name = name,
                        author = author,
                        primary = primary.toArgb().toLong()
                    )
                    viewModel.saveAccent(accent) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        }

    if (pickingColor) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingColor = false },
            containerColor = opaqueColorScheme.surface,
            confirmButton = {
                Button(onClick = { pickingColor = false }) { Text("OK") }
            },
            text = {
                ColorPicker(initialColor = primary) { primary = it }
            }
        )
    }
}

