package com.deliriousvoid.openvkmatcha.ui.screens.profile

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EditProfileTab
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EditProfileUiState
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EditProfileViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl

    DisposableEffect(state.currentTab) {
        AppEvents.setTopBarState(TopBarState(
            route = Routes.EDIT_PROFILE,
            customTopBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    Column {
                        CenterAlignedTopAppBar(
                            title = { Text("Редактирование профиля") },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад"
                                    )
                                }
                            }
                        )
                        PrimaryTabRow(selectedTabIndex = state.currentTab.ordinal) {
                            Tab(
                                selected = state.currentTab == EditProfileTab.MAIN,
                                onClick = { viewModel.setTab(EditProfileTab.MAIN) },
                                text = { Text("Основное") }
                            )
                            Tab(
                                selected = state.currentTab == EditProfileTab.CONTACTS,
                                onClick = { viewModel.setTab(EditProfileTab.CONTACTS) },
                                text = { Text("Контакты") }
                            )
                            Tab(
                                selected = state.currentTab == EditProfileTab.INTERESTS,
                                onClick = { viewModel.setTab(EditProfileTab.INTERESTS) },
                                text = { Text("Интересы") }
                            )
                            Tab(
                                selected = state.currentTab == EditProfileTab.ADDITIONAL,
                                onClick = { viewModel.setTab(EditProfileTab.ADDITIONAL) },
                                text = { Text("Доп.") }
                            )
                        }
                    }
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onBack()
        }
    }

    Scaffold { padding ->
        when (state.currentTab) {
            EditProfileTab.MAIN -> MainTabContent(padding, state, viewModel)
            EditProfileTab.CONTACTS -> WebViewTabContent(padding, "$baseUrl/edit?act=contacts")
            EditProfileTab.INTERESTS -> InterestsTabContent(padding, state, viewModel)
            EditProfileTab.ADDITIONAL -> WebViewTabContent(padding, "$baseUrl/edit?act=additional")
        }
    }
}

@Composable
private fun MainTabContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: EditProfileUiState,
    viewModel: EditProfileViewModel
) {
    if (state.isLoading) {
        LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.error != null) {
                ErrorText(message = state.error, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            AvatarSection(state, viewModel)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.info.firstName,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(firstName = newValue) } },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.lastName,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(lastName = newValue) } },
                label = { Text("Фамилия") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.screenName,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(screenName = newValue) } },
                label = { Text("Короткое имя (shortcode)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.status,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(status = newValue) } },
                label = { Text("Статус") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            SexDropdown(
                selectedSex = state.info.sex,
                onSexSelected = { newValue -> viewModel.updateInfo { it.copy(sex = newValue) } }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.bdate,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(bdate = newValue) } },
                label = { Text("День рождения (Д.М.ГГГГ)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            BdateVisibilityDropdown(
                selectedVisibility = state.info.bdateVisibility,
                onVisibilitySelected = { newValue -> viewModel.updateInfo { it.copy(bdateVisibility = newValue) } }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.homeTown,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(homeTown = newValue) } },
                label = { Text("Родной город") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить изменения")
                }
            }
        }
    }
}

@Composable
private fun AvatarSection(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(it, contentResolver) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { imagePicker.launch("image/*") }
        ) {
            AsyncImage(
                model = state.newAvatarUri ?: state.info.photo200,
                contentDescription = "Аватарка",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (state.isAvatarUploading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = { imagePicker.launch("image/*") }) {
            Text("Сменить аватар")
        }
    }
    
    state.avatarUploadError?.let {
        Spacer(modifier = Modifier.height(8.dp))
        ErrorText(message = it, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InterestsTabContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: EditProfileUiState,
    viewModel: EditProfileViewModel
) {
    if (state.isLoading) {
        LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.error != null) {
                ErrorText(message = state.error, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = state.info.about,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(about = newValue) } },
                label = { Text("О себе") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.interests,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(interests = newValue) } },
                label = { Text("Интересы") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.music,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(music = newValue) } },
                label = { Text("Любимая музыка") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.movies,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(movies = newValue) } },
                label = { Text("Любимые фильмы") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.tv,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(tv = newValue) } },
                label = { Text("Любимые телешоу") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.books,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(books = newValue) } },
                label = { Text("Любимые книги") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.games,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(games = newValue) } },
                label = { Text("Любимые игры") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.info.quotes,
                onValueChange = { newValue -> viewModel.updateInfo { it.copy(quotes = newValue) } },
                label = { Text("Любимые цитаты") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить изменения")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewTabContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    url: String
) {
    val token = OpenVKMatchaApp.instance.api.token
    
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                
                // Add token to headers if needed, or just let it load
                // OpenVK usually expects cookies or token in params for some instances
                // But if it's a web view, it might need session.
                val headers = mutableMapOf<String, String>()
                token?.let { headers["Authorization"] = "Bearer $it" }
                
                loadUrl(url, headers)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexDropdown(
    selectedSex: Int,
    onSexSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sexOptions = listOf("Не указан" to 0, "Женский" to 1, "Мужской" to 2)
    val selectedOption = sexOptions.find { it.second == selectedSex }?.first ?: "Не указан"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text("Пол") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sexOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first) },
                    onClick = {
                        onSexSelected(option.second)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BdateVisibilityDropdown(
    selectedVisibility: Int,
    onVisibilitySelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "Показывать дату рождения" to 1,
        "Показывать только месяц и день" to 2
        // Visibility 0 (Hide) is not supported by OpenVK and causes HTTP 400
    )
    val selectedOption = options.find { it.second == selectedVisibility }?.first ?: "Показывать дату рождения"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text("Видимость даты рождения") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first) },
                    onClick = {
                        onVisibilitySelected(option.second)
                        expanded = false
                    }
                )
            }
        }
    }
}
