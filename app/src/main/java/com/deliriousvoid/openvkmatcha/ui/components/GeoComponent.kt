package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.data.model.Geo

@Composable
fun GeoComponent(
    geo: Geo,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val place = geo.place
    val title = place?.title?.takeIf { it.isNotBlank() }
        ?: if (!geo.coordinates.isNullOrBlank()) {
            "Местоположение: ${geo.coordinates}"
        } else {
            "Местоположение"
        }
    
    val addressParts = mutableListOf<String>()
    if (place != null) {
        // If title is "Место" or coordinates, try to use city/address as secondary info
        // Otherwise only add if they differ from title
        val isGenericTitle = title == "Место" || title.startsWith("Местоположение")
        
        place.city?.takeIf { it.isNotBlank() && (isGenericTitle || it != title) }?.let { addressParts.add(it) }
        place.address?.takeIf { it.isNotBlank() && (isGenericTitle || it != title) }?.let { addressParts.add(it) }
        place.country?.takeIf { it.isNotBlank() && (isGenericTitle || it != title) }?.let { addressParts.add(it) }
    }
    val address = addressParts.distinct().joinToString(", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (address.isNotBlank()) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
