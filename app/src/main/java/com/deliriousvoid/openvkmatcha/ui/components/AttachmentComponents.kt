package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.data.model.*

@Composable
fun CommentAttachmentsList(
    imageUrls: List<String>,
    videos: List<Video>,
    audios: List<AudioTrack>,
    documents: List<Document>,
    poll: Poll?,
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onAudioToggleAdded: (AudioTrack) -> Unit = {},
    onAudioDownload: (AudioTrack) -> Unit = {},
    onAudioShare: (AudioTrack) -> Unit = {},
    onAudioAddToQueue: (AudioTrack) -> Unit = {},
    onAudioPlayNext: (AudioTrack) -> Unit = {},
    currentTrack: AudioTrack? = null,
    isDownloaded: (Int, Int) -> Boolean = { _, _ -> false },
    getTrackState: (AudioTrack) -> AudioTrack = { it },
    onDocumentDownload: (Document) -> Unit = {},
    onPollVote: (List<Int>) -> Unit = {},
    onImageClick: (Int) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            PostImageGrid(
                imageUrls = imageUrls,
                onImageClick = onImageClick
            )
        }

        val gifDocs = documents.filter { it.ext.lowercase() == "gif" || it.type == 3 }
        val otherDocs = documents.filterNot { it.ext.lowercase() == "gif" || it.type == 3 }

        if (gifDocs.isNotEmpty()) {
            gifDocs.forEach { gif ->
                Spacer(modifier = Modifier.height(8.dp))
                GifPostItem(
                    document = gif,
                    onClick = { onImageClick(imageUrls.size + gifDocs.indexOf(gif)) },
                    onDownload = { onDocumentDownload(gif) }
                )
            }
        }

        if (otherDocs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                otherDocs.forEach { doc ->
                    DocumentItem(
                        document = doc,
                        onClick = { onDocumentDownload(doc) }
                    )
                }
            }
        }

        if (videos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            videos.forEach { video ->
                VideoPlayer(
                    video = video,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (poll != null) {
            Spacer(modifier = Modifier.height(8.dp))
            PollComponent(
                poll = poll,
                onVote = onPollVote
            )
        }

        if (audios.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                audios.forEachIndexed { index, track ->
                    val displayTrack = getTrackState(track)
                    AudioTrackItem(
                        track = displayTrack,
                        onClick = { onAudioClick(audios, index) },
                        onToggleAdded = { onAudioToggleAdded(displayTrack) },
                        onDownload = { onAudioDownload(displayTrack) },
                        onShare = { onAudioShare(displayTrack) },
                        onAddToQueue = { onAudioAddToQueue(displayTrack) },
                        onPlayNext = { onAudioPlayNext(displayTrack) },
                        isSelected = displayTrack.stableId == currentTrack?.stableId,
                        isDownloaded = isDownloaded(displayTrack.id, displayTrack.ownerId)
                    )
                    if (index < audios.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}
