package com.deliriousvoid.openvkmatcha.ui.screens.settings

import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.viewmodel.AppIconVariant
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@Composable
fun AppIconSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentIcon by viewModel.currentIcon.collectAsState()

    val titleLabel = matchaStringResource(R.string.app_icon)
    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(
            title = titleLabel,
            route = Routes.SETTINGS_APP_ICON
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppIconVariant.entries.forEach { variant ->
            IconVariantRow(
                variant = variant,
                isSelected = currentIcon == variant,
                onClick = { viewModel.setAppIcon(variant) }
            )
        }
    }
}

@Composable
private fun IconVariantRow(
    variant: AppIconVariant,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(variant.iconRes)
                    .build()
            ),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = variant.title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
