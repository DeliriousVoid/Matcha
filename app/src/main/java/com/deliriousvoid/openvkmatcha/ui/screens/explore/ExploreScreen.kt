package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ExploreViewModel

data class ExploreFeature(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val color: Color = Color.Transparent
)

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onOpenFeature: (String) -> Unit,
    onOpenWebView: (String, String) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val greeting = viewModel.getGreeting()
    val baseUrl = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Greeting Header
        state.user?.let { user ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { onOpenProfile(user.id) }
            ) {
                AsyncImage(
                    model = user.photo200,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "$greeting,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Features Grid
        val features = listOf(
            ExploreFeature("Музыка", Icons.Default.MusicNote, { onOpenFeature("music") }),
            ExploreFeature("Видео", Icons.Default.Movie, { onOpenFeature("video") }),
            ExploreFeature("Документы", Icons.Default.Description, { onOpenFeature("docs") }),
            ExploreFeature("Друзья", Icons.Default.People, { onOpenFeature("friends") }),
            ExploreFeature("Сообщества", Icons.Default.Groups, { onOpenFeature("groups") }),
            ExploreFeature("События", Icons.Default.Event, { onOpenFeature("events") }),
            ExploreFeature("Заметки", Icons.AutoMirrored.Filled.StickyNote2, { onOpenFeature("notes") }),
            ExploreFeature("Сообщения", Icons.AutoMirrored.Filled.Chat, { onOpenFeature("messages") }),
            ExploreFeature("Игры", Icons.Default.VideogameAsset, { 
                onOpenWebView("$baseUrl/search?q=&section=apps", "Игры") 
            })
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            features.chunked(3).forEach { rowFeatures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowFeatures.forEach { feature ->
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureItem(feature)
                        }
                    }
                    // Fill empty slots if last row has < 3 items
                    if (rowFeatures.size < 3) {
                        repeat(3 - rowFeatures.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Wallet Section
        WalletSection(
            balance = state.balance,
            onTopUp = { onOpenWebView("$baseUrl/settings?act=finance", "Пополнение баланса") },
            onVoucher = { onOpenWebView("$baseUrl/settings?act=finance", "Ваучер") },
            onTransfer = { onOpenFeature("transfer") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FeatureItem(feature: ExploreFeature) {
    Surface(
        onClick = feature.onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WalletSection(
    balance: Int,
    onTopUp: () -> Unit,
    onVoucher: () -> Unit,
    onTransfer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Кошелёк",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "$balance голосов",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WalletButton(
                    label = "Пополнить",
                    icon = Icons.Default.Add,
                    onClick = onTopUp,
                    modifier = Modifier.weight(1f)
                )
                WalletButton(
                    label = "Ваучер",
                    icon = Icons.Default.ConfirmationNumber,
                    onClick = onVoucher,
                    modifier = Modifier.weight(1f)
                )
                WalletButton(
                    label = "Перевести",
                    icon = Icons.AutoMirrored.Filled.Send,
                    onClick = onTransfer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WalletButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
        }
    }
}
