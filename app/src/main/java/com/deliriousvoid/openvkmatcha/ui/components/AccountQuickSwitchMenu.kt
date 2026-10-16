package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.security.AccountManager
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource

class AboveAnchorPositionProvider(
    private val xOffsetPx: Int = 0,
    private val yOffsetPx: Int = 0
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width + xOffsetPx)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.top - popupContentSize.height + yOffsetPx)
            .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

@Composable
fun AccountQuickSwitchMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    accounts: List<AccountManager.Account>,
    currentAccountId: String?,
    onSelectAccount: (AccountManager.Account) -> Unit,
    onAddAccount: () -> Unit
) {
    if (!expanded) return

    val density = LocalDensity.current
    val xOffsetPx = with(density) { (-8).dp.roundToPx() }
    val yOffsetPx = with(density) { (-8).dp.roundToPx() }

    Popup(
        popupPositionProvider = AboveAnchorPositionProvider(xOffsetPx, yOffsetPx),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 220.dp, max = 280.dp)
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                // Add Account item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismissRequest()
                            onAddAccount()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = matchaStringResource(R.string.add_account),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = matchaStringResource(R.string.add_account),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Accounts list
                accounts.forEach { account ->
                    val isSelected = account.id == currentAccountId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismissRequest()
                                if (!isSelected) {
                                    onSelectAccount(account)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = account.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                        } else Modifier
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
