package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.data.model.Poll
import com.deliriousvoid.openvkmatcha.util.StringUtils

@Composable
fun PollComponent(
    poll: Poll,
    onVote: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isVoted = poll.answerIds.isNotEmpty()
    val showsResults = isVoted || poll.closed || !poll.canVote
    var selectedAnswers by remember(poll.id, poll.answerIds) { mutableStateOf(poll.answerIds) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = poll.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val pollInfo = buildString {
            if (poll.anonymous) append("Анонимный опрос") else append("Публичный опрос")
            if (poll.multiple) append(" • Выбор нескольких вариантов")
        }
        Text(
            text = pollInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        poll.answers.forEach { answer ->
            val isAnswerSelected = selectedAnswers.contains(answer.id)
            val percentage = (answer.rate / 100.0).toFloat()
            val animatedProgress by animateFloatAsState(targetValue = if (showsResults) percentage else 0f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(enabled = poll.canVote && !poll.closed) {
                        if (poll.multiple) {
                            selectedAnswers = if (isAnswerSelected) {
                                selectedAnswers - answer.id
                            } else {
                                selectedAnswers + answer.id
                            }
                        } else {
                            if (!isVoted) {
                                onVote(listOf(answer.id))
                            }
                        }
                    }
            ) {
                if (showsResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                if (isAnswerSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!showsResults && poll.canVote) {
                        Icon(
                            imageVector = if (poll.multiple) {
                                if (isAnswerSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
                            } else {
                                if (isAnswerSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isAnswerSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (showsResults && isAnswerSelected) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = answer.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isAnswerSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    if (showsResults) {
                        Text(
                            text = "${answer.rate.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnswerSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (poll.multiple && !showsResults && poll.canVote && selectedAnswers.isNotEmpty()) {
            Button(
                onClick = { onVote(selectedAnswers) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Проголосовать")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = StringUtils.getPollVotesString(poll.votes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isVoted && !poll.closed) {
                TextButton(
                    onClick = { onVote(emptyList()) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Отменить голос",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (poll.closed) {
                Text(
                    text = "Опрос завершён",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
