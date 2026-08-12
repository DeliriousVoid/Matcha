package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import com.deliriousvoid.openvkmatcha.ui.components.DocsPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.MusicPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.PendingAttachmentItem
import com.deliriousvoid.openvkmatcha.ui.components.VideoPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    ownerId: Int,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onOpenMap: (lat: Double?, lon: Double?) -> Unit = { _, _ -> },
    onOpenGraffiti: () -> Unit = {},
    resultLocation: Triple<Double, Double, String?>? = null,
    resultGraffiti: android.net.Uri? = null,
    viewModel: CreatePostViewModel = viewModel(factory = CreatePostViewModel.factory(ownerId))
) {
    val state by viewModel.uiState.collectAsState()
    var showMusicPicker by remember { mutableStateOf(false) }
    var showVideoPicker by remember { mutableStateOf(false) }
    var showDocsPicker by remember { mutableStateOf(false) }
    var showLocationNameDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempLocationName by remember { mutableStateOf("") }
    var tempSourceUrl by remember { mutableStateOf("") }

    LaunchedEffect(resultLocation) {
        if (resultLocation != null) {
            viewModel.setLocation(resultLocation.first, resultLocation.second, resultLocation.third)
        }
    }

    LaunchedEffect(resultGraffiti) {
        if (resultGraffiti != null) {
            viewModel.addAttachment(resultGraffiti, AttachmentType.GRAFFITI, name = "graffiti_${System.currentTimeMillis()}.png")
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PHOTO) }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая запись") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    if (state.isSending) {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.post() },
                            enabled = state.inputText.isNotBlank() || state.pendingAttachments.isNotEmpty() || state.pollQuestion != null
                        ) {
                            Text("Опубликовать", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            TextField(
                value = state.inputText,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 150.dp),
                placeholder = { Text("Что у вас нового?") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (state.pendingAttachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.pendingAttachments) { attachment ->
                        PendingAttachmentItem(
                            attachment = attachment,
                            onRemove = { viewModel.removeAttachment(attachment) }
                        )
                    }
                }
            }

            if (state.latitude != null && state.longitude != null) {
                AssistChip(
                    onClick = { 
                        tempLocationName = state.locationName ?: ""
                        showLocationNameDialog = true 
                    },
                    label = { Text(state.locationName ?: "Местоположение: ${"%.4f".format(state.latitude)}, ${"%.4f".format(state.longitude)}") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.setLocation(null, null) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.pollQuestion != null) {
                AssistChip(
                    onClick = { showPollDialog = true },
                    label = { Text("Опрос: ${state.pollQuestion}") },
                    leadingIcon = { Icon(Icons.Default.Poll, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.removePoll() }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.copyright != null) {
                AssistChip(
                    onClick = { 
                        tempSourceUrl = state.copyright ?: ""
                        showSourceDialog = true 
                    },
                    label = { Text("Источник: ${state.copyright}") },
                    leadingIcon = { Icon(Icons.Default.Link, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateCopyright(null) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(onClick = { photoPicker.launch("image/*") }) {
                    Icon(Icons.Default.PhotoCamera, "Фото", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOpenGraffiti) {
                    Icon(Icons.Default.Gesture, "Граффити", tint = MaterialTheme.colorScheme.primary)
                }
                if (state.isDeveloperMode) {
                    IconButton(onClick = { showVideoPicker = true }) {
                        Icon(Icons.Default.PlayCircle, "Видео", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { showMusicPicker = true }) {
                    Icon(Icons.Default.MusicNote, "Аудио", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showPollDialog = true }) {
                    Icon(Icons.Default.Poll, "Опрос", tint = MaterialTheme.colorScheme.primary)
                }
                if (state.isDeveloperMode) {
                    IconButton(onClick = { showDocsPicker = true }) {
                        Icon(Icons.Default.Description, "Документ", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { 
                        tempSourceUrl = state.copyright ?: ""
                        showSourceDialog = true 
                    }) {
                        Icon(Icons.Default.Link, "Источник", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { onOpenMap(state.latitude, state.longitude) }) {
                    Icon(Icons.Default.LocationOn, "Местоположение", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (state.isGroup && state.isAdmin) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setFromGroup(!state.fromGroup) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.fromGroup,
                            onCheckedChange = { viewModel.setFromGroup(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("От имени сообщества", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.fromGroup) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSigned(!state.signed) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.signed,
                                onCheckedChange = { viewModel.setSigned(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Подпись автора", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setNsfw(!state.isNsfw) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.isNsfw,
                            onCheckedChange = { viewModel.setNsfw(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Пометить как NSFW", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.setNsfw(!state.isNsfw) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.isNsfw,
                        onCheckedChange = { viewModel.setNsfw(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Пометить как NSFW", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelect = { track ->
                viewModel.addExistingAttachment(
                    attachmentString = "audio${track.ownerId}_${track.id}",
                    type = AttachmentType.AUDIO,
                    name = "${track.artist} - ${track.title}"
                )
                showMusicPicker = false
            }
        )
    }

    if (showVideoPicker) {
        VideoPickerBottomSheet(
            onDismiss = { showVideoPicker = false },
            onVideoSelect = { video ->
                viewModel.addExistingAttachment(
                    attachmentString = "video${video.ownerId}_${video.id}",
                    type = AttachmentType.VIDEO,
                    name = video.title
                )
                showVideoPicker = false
            }
        )
    }

    if (showDocsPicker) {
        DocsPickerBottomSheet(
            onDismiss = { showDocsPicker = false },
            onDocSelect = { doc ->
                viewModel.addExistingAttachment(
                    attachmentString = "document${doc.ownerId}_${doc.id}",
                    type = AttachmentType.DOCUMENT,
                    name = doc.title
                )
                showDocsPicker = false
            }
        )
    }

    if (showLocationNameDialog) {
        AlertDialog(
            onDismissRequest = { showLocationNameDialog = false },
            title = { Text("Название места") },
            text = {
                OutlinedTextField(
                    value = tempLocationName,
                    onValueChange = { tempLocationName = it },
                    label = { Text("Введите название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateLocationName(tempLocationName.ifBlank { null })
                    showLocationNameDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationNameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Источник") },
            text = {
                OutlinedTextField(
                    value = tempSourceUrl,
                    onValueChange = { tempSourceUrl = it },
                    label = { Text("Введите ссылку") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateCopyright(tempSourceUrl.ifBlank { null })
                    showSourceDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showPollDialog) {
        PollCreationDialog(
            initialQuestion = state.pollQuestion ?: "",
            initialAnswers = if (state.pollAnswers.isEmpty()) listOf("", "") else state.pollAnswers,
            initialAnonymous = state.pollAnonymous,
            initialMultiple = state.pollMultiple,
            initialDisableUnvote = state.pollDisableUnvote,
            initialEndDate = state.pollEndDate,
            onDismiss = { showPollDialog = false },
            onConfirm = { question, answers, anonymous, multiple, disableUnvote, endDate ->
                viewModel.updatePoll(question, answers, anonymous, multiple, disableUnvote, endDate)
                showPollDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreationDialog(
    initialQuestion: String,
    initialAnswers: List<String>,
    initialAnonymous: Boolean,
    initialMultiple: Boolean,
    initialDisableUnvote: Boolean,
    initialEndDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, Boolean, Boolean, Boolean, Long?) -> Unit
) {
    var question by remember { mutableStateOf(initialQuestion) }
    var answers by remember { mutableStateOf(initialAnswers.toMutableList()) }
    var anonymous by remember { mutableStateOf(initialAnonymous) }
    var multiple by remember { mutableStateOf(initialMultiple) }
    var disableUnvote by remember { mutableStateOf(initialDisableUnvote) }
    var endDateOption by remember { mutableStateOf(0) } // 0: No limit, 1: 1h, 2: 1d, 3: 1w

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Опрос") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Тема опроса") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Варианты ответов", style = MaterialTheme.typography.titleSmall)
                
                answers.forEachIndexed { index, answer ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { 
                                val newAnswers = answers.toMutableList()
                                newAnswers[index] = it
                                answers = newAnswers
                            },
                            label = { Text("Вариант ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (answers.size > 2) {
                            IconButton(onClick = {
                                val newAnswers = answers.toMutableList()
                                newAnswers.removeAt(index)
                                answers = newAnswers
                            }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        }
                    }
                }
                
                if (answers.size < 10) {
                    TextButton(
                        onClick = {
                            val newAnswers = answers.toMutableList()
                            newAnswers.add("")
                            answers = newAnswers
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить вариант")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { anonymous = !anonymous },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = anonymous, onCheckedChange = { anonymous = it })
                    Text("Анонимный опрос", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { multiple = !multiple },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = multiple, onCheckedChange = { multiple = it })
                    Text("Выбор нескольких вариантов", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { disableUnvote = !disableUnvote },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = disableUnvote, onCheckedChange = { disableUnvote = it })
                    Text("Запретить отмену голоса", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Срок опроса", style = MaterialTheme.typography.titleSmall)
                val options = listOf("Без ограничений", "1 час", "1 день", "1 неделя")
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { endDateOption = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = endDateOption == index, onClick = { endDateOption = index })
                        Text(option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedQuestion = question.trim()
                    val filteredAnswers = answers.map { it.trim() }.filter { it.isNotBlank() }
                    if (trimmedQuestion.isNotBlank() && filteredAnswers.size >= 2) {
                        val now = System.currentTimeMillis() / 1000
                        val endDate = when (endDateOption) {
                            1 -> now + 3600
                            2 -> now + 86400
                            3 -> now + 604800
                            else -> null
                        }
                        onConfirm(trimmedQuestion, filteredAnswers, anonymous, multiple, disableUnvote, endDate)
                    }
                },
                enabled = question.trim().isNotBlank() && answers.count { it.trim().isNotBlank() } >= 2
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
