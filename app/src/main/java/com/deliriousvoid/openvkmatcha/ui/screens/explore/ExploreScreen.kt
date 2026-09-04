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
import androidx.compose.runtime.remember
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
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.theme.LocalExtraColors
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

        val musicTitle = matchaStringResource(R.string.explore_music)
        val videosTitle = matchaStringResource(R.string.title_videos)
        val docsTitle = matchaStringResource(R.string.title_documents)
        val friendsTitle = matchaStringResource(R.string.title_friends)
        val groupsTitle = matchaStringResource(R.string.title_groups)
        val eventsTitle = matchaStringResource(R.string.title_events)
        val notesTitle = matchaStringResource(R.string.title_notes)
        val chatsTitle = matchaStringResource(R.string.explore_chats)
        val gamesTitle = matchaStringResource(R.string.explore_games)

        // Features Grid
        val features = remember(musicTitle, videosTitle, docsTitle, friendsTitle, groupsTitle, eventsTitle, notesTitle, chatsTitle, gamesTitle) {
            listOf(
                ExploreFeature(musicTitle, Icons.Default.MusicNote, { onOpenFeature("music") }),
                ExploreFeature(videosTitle, Icons.Default.Movie, { onOpenFeature("video") }),
                ExploreFeature(docsTitle, Icons.Default.Description, { onOpenFeature("docs") }),
                ExploreFeature(friendsTitle, Icons.Default.People, { onOpenFeature("friends") }),
                ExploreFeature(groupsTitle, Icons.Default.Groups, { onOpenFeature("groups") }),
                ExploreFeature(eventsTitle, Icons.Default.Event, { onOpenFeature("events") }),
                ExploreFeature(notesTitle, Icons.AutoMirrored.Filled.StickyNote2, { onOpenFeature("notes") }),
                ExploreFeature(chatsTitle, Icons.AutoMirrored.Filled.Chat, { onOpenFeature("messages") }),
                ExploreFeature(gamesTitle, Icons.Default.VideogameAsset, { 
                    onOpenWebView("$baseUrl/search?q=&section=apps", gamesTitle) 
                })
            )
        }

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

        val topUpTitle = matchaStringResource(R.string.explore_wallet_top_up_title)
        val voucherTitle = matchaStringResource(R.string.explore_voucher)

        // Wallet Section
        WalletSection(
            balance = state.balance,
            onTopUp = { onOpenWebView("$baseUrl/settings?act=finance", topUpTitle) },
            onVoucher = { onOpenWebView("$baseUrl/settings?act=finance", voucherTitle) },
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
            containerColor = LocalExtraColors.current.wallet.copy(alpha = 0.8f)
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
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = matchaStringResource(R.string.explore_wallet),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Text(
                    text = matchaStringResource(R.string.explore_balance_format, balance),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WalletButton(
                    label = matchaStringResource(R.string.explore_top_up),
                    icon = Icons.Default.Add,
                    onClick = onTopUp,
                    modifier = Modifier.weight(1f)
                )
                WalletButton(
                    label = matchaStringResource(R.string.explore_voucher),
                    icon = Icons.Default.ConfirmationNumber,
                    onClick = onVoucher,
                    modifier = Modifier.weight(1f)
                )
                WalletButton(
                    label = matchaStringResource(R.string.explore_transfer),
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
