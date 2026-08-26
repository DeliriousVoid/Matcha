package com.deliriousvoid.openvkmatcha.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.LinkHandler

import com.deliriousvoid.openvkmatcha.ui.util.MarkdownParser

@Composable
fun ParsedText(
    text: String,
    onMentionClick: (Int) -> Unit,
    onUrlClick: (String) -> Unit = {},
    onWallClick: (Int, Int) -> Unit = { _, _ -> },
    onProfileClick: (String) -> Unit = {},
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onDefaultClick: () -> Unit = {},
    onDoubleTap: ((androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var selectedAnnotation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pressedRange by remember { mutableStateOf<TextRange?>(null) }

    val currentOnMentionClick by rememberUpdatedState(onMentionClick)
    val currentOnUrlClick by rememberUpdatedState(onUrlClick)
    val currentOnWallClick by rememberUpdatedState(onWallClick)
    val currentOnProfileClick by rememberUpdatedState(onProfileClick)
    val currentOnMusicClick by rememberUpdatedState(onMusicClick)
    val currentOnPlaylistClick by rememberUpdatedState(onPlaylistClick)
    val currentOnDefaultClick by rememberUpdatedState(onDefaultClick)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnTextLayout by rememberUpdatedState(onTextLayout)

    val tags = listOf("MENTION", "PROFILE", "WALL", "MUSIC", "PLAYLIST", "URL")

    val handleAnnotation = { tag: String, item: String ->
        when (tag) {
            "MENTION" -> currentOnMentionClick(item.toIntOrNull() ?: 0)
            "PROFILE" -> currentOnProfileClick(item)
            "WALL" -> {
                val parts = item.split("_")
                if (parts.size == 2) {
                    currentOnWallClick(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                }
            }
            "MUSIC" -> currentOnMusicClick(item.toIntOrNull() ?: 0)
            "PLAYLIST" -> {
                val parts = item.split("_")
                if (parts.size == 2) {
                    currentOnPlaylistClick(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                }
            }
            "URL" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    currentOnUrlClick(item)
                }
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text, primaryColor) {
        MarkdownParser.parse(text, primaryColor)
    }

    Box {
        SelectionContainer {
            Text(
                text = annotatedString,
                modifier = modifier
                    .drawBehind {
                        pressedRange?.let { range ->
                            layoutResult?.let { lr ->
                                val path = lr.getPathForRange(range.start, range.end)
                                drawPath(
                                    path = path,
                                    color = color.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                    .pointerInput(annotatedString, onDoubleTap == null) {
                        detectTapGestures(
                            onPress = { offset ->
                                layoutResult?.let { lr ->
                                    val pos = lr.getOffsetForPosition(offset)
                                    val annotation = tags
                                        .mapNotNull { tag ->
                                            annotatedString
                                                .getStringAnnotations(tag, pos, pos)
                                                .firstOrNull()
                                        }
                                        .firstOrNull()

                                    if (annotation != null) {
                                        pressedRange = TextRange(annotation.start, annotation.end)
                                    }
                                }
                                try {
                                    awaitRelease()
                                } finally {
                                    pressedRange = null
                                }
                            },
                            onTap = { offset ->
                                layoutResult?.let { lr ->
                                    val pos = lr.getOffsetForPosition(offset)
                                    val annotation = tags
                                        .mapNotNull { tag ->
                                            annotatedString
                                                .getStringAnnotations(tag, pos, pos)
                                                .firstOrNull()
                                                ?.let { tag to it.item }
                                        }
                                        .firstOrNull()

                                    if (annotation != null) {
                                        handleAnnotation(annotation.first, annotation.second)
                                    } else {
                                        currentOnDefaultClick()
                                    }
                                }
                            },
                            onDoubleTap = if (onDoubleTap != null) { offset ->
                                currentOnDoubleTap?.invoke(offset)
                            } else null,
                            onLongPress = { offset ->
                                layoutResult?.let { lr ->
                                    val pos = lr.getOffsetForPosition(offset)
                                    val annotation = tags
                                        .mapNotNull { tag ->
                                            annotatedString
                                                .getStringAnnotations(tag, pos, pos)
                                                .firstOrNull()
                                                ?.let { tag to it.item }
                                        }
                                        .firstOrNull()

                                    if (annotation != null) {
                                        val url = when (annotation.first) {
                                            "URL" -> annotation.second
                                            "PROFILE" -> "https://openvk.org/${if (annotation.second.startsWith("-")) "club${annotation.second.substring(1)}" else if (annotation.second.all { it.isDigit() }) "id${annotation.second}" else annotation.second}"
                                            "WALL" -> "https://openvk.org/wall${annotation.second}"
                                            "MUSIC" -> "https://openvk.org/audios${annotation.second}"
                                            "PLAYLIST" -> "https://openvk.org/playlist${annotation.second}"
                                            "MENTION" -> {
                                                val id = annotation.second.toIntOrNull() ?: 0
                                                if (id > 0) "https://openvk.org/id$id" else "https://openvk.org/club${-id}"
                                            }

                                            else -> null
                                        }
                                        if (url != null) {
                                            selectedUrl = url
                                            selectedAnnotation = annotation
                                            menuExpanded = true
                                        }
                                    }
                                }
                            }
                        )
                    },
                style = style.copy(color = color),
                maxLines = maxLines,
                overflow = overflow,
                onTextLayout = {
                    layoutResult = it
                    currentOnTextLayout(it)
                }
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Копировать весь текст") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(text))
                    menuExpanded = false
                }
            )
            selectedUrl?.let { url ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_link)) },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(url))
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open_in_browser)) },
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        menuExpanded = false
                    }
                )
                
                val isMatchaLink = LinkHandler.getRouteForUrl(url) != null
                if (isMatchaLink) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_in_matcha)) },
                        onClick = {
                            selectedAnnotation?.let { (tag, item) ->
                                handleAnnotation(tag, item)
                            }
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
