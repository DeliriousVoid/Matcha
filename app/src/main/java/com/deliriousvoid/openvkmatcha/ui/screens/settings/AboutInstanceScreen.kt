package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.InstanceInfo
import com.deliriousvoid.openvkmatcha.data.model.InstanceLink
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.OnlineIndicator
import com.deliriousvoid.openvkmatcha.ui.components.VerifiedBadge
import com.deliriousvoid.openvkmatcha.ui.viewmodel.AboutInstanceViewModel

@Composable
fun AboutInstanceScreen(
    onBack: () -> Unit,
    onOpenProfile: (Any) -> Unit,
    viewModel: AboutInstanceViewModel = viewModel(factory = AboutInstanceViewModel.factory())
) {
    val instanceInfo by viewModel.instanceInfo.collectAsState()
    val developers by viewModel.developers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && instanceInfo == null) {
            LoadingBox(modifier = Modifier.fillMaxSize())
        } else if (error != null && instanceInfo == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadData() }) {
                    Text("Повторить")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                instanceInfo?.let { info ->
                    item {
                        InstanceHeader(info)
                    }

                    info.statistics?.let { stats ->
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Статистика",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            StatisticsGrid(stats)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Matcha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(developers) { dev ->
                        UserItem(user = dev, onClick = { onOpenProfile(dev.id) })
                    }

                    info.administrators?.let { admins ->
                        if (admins.items.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Администраторы",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(admins.items) { admin ->
                                UserItem(user = admin, onClick = { onOpenProfile(admin.id) })
                            }
                        }
                    }

                    info.links?.let { links ->
                        if (links.items.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Ссылки",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(links.items) { link ->
                                LinkItem(link = link)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstanceHeader(info: InstanceInfo) {
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl
    val displayUrl = baseUrl.replace("https://", "").replace("http://", "")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = info.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = displayUrl,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (info.openvkVersion.isNotBlank()) {
            Text(
                text = "Версия OpenVK: ${info.openvkVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (info.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatisticsGrid(stats: com.deliriousvoid.openvkmatcha.data.model.InstanceStatistics) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem("Пользователей", stats.usersCount.toString(), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatItem("Онлайн", stats.onlineUsersCount.toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem("Активных", stats.activeUsersCount.toString(), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatItem("Сообществ", stats.groupsCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UserItem(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = user.photo200,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            OnlineIndicator(
                isOnline = user.online,
                isMobile = user.mobileOnline,
                modifier = Modifier.align(Alignment.BottomEnd),
                dotSize = 10.dp,
                iconSize = 14.dp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (user.verified || com.deliriousvoid.openvkmatcha.Constants.CUSTOM_VERIFIED_IDS.contains(user.id)) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(userId = user.id, isVerified = user.verified, size = 16.dp)
                }
            }
            if (user.screenName.isNotBlank()) {
                Text(
                    text = "@${user.screenName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinkItem(link: InstanceLink) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(link.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                link.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
