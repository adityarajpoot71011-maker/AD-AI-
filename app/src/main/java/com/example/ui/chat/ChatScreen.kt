package com.example.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.AiBubbleBg
import com.example.ui.theme.BackgroundGradient
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardDarkVariant
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassInputGradient
import com.example.ui.theme.NeonBadgeGradient
import com.example.ui.theme.PurpleBlueGradient
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleStart
import com.example.ui.theme.VoidDark
import com.example.util.SpeechLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val inputPrompt by viewModel.inputPrompt.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val speechLanguage by viewModel.speechLanguage.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()

    val speechError by viewModel.speechManager.speechError.collectAsState()

    // Activity Result Launcher for Photo Picker (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.attachedImageUri.value = uri
    }

    // Permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speech error toast
    LaunchedEffect(speechError) {
        speechError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSelectSession = { id ->
                    viewModel.selectSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id ->
                    viewModel.deleteSession(id)
                },
                onClearAllChats = {
                    viewModel.clearAllChats()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdAiLogoBadge()
                            Column {
                                Text(
                                    text = "AD AI",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hindi & English",
                                    fontSize = 10.sp,
                                    color = ElectricBlue
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open chat history",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Web Search Toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isWebSearchEnabled) DeepViolet.copy(alpha = 0.35f) else CardDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isWebSearchEnabled) ElectricPurple else CardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.isWebSearchEnabled.value = !isWebSearchEnabled }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("web_search_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWebSearchEnabled) Icons.Default.Public else Icons.Default.PublicOff,
                                    contentDescription = "Toggle Web Search",
                                    tint = if (isWebSearchEnabled) ElectricBlue else TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Web",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isWebSearchEnabled) Color.White else TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Language Preference Dropdown
                        var showLangMenu by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showLangMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("language_selector")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = when (preferredLanguage) {
                                            "HI" -> "हिंदी"
                                            "EN" -> "EN"
                                            else -> "Auto"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricPurple
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Auto (Hindi & English)") },
                                    onClick = {
                                        viewModel.preferredLanguage.value = "AUTO"
                                        showLangMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("हिंदी (Hindi)") },
                                    onClick = {
                                        viewModel.preferredLanguage.value = "HI"
                                        showLangMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("English (EN)") },
                                    onClick = {
                                        viewModel.preferredLanguage.value = "EN"
                                        showLangMenu = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // New Chat Button
                        IconButton(
                            onClick = { viewModel.createNewChat() },
                            modifier = Modifier.testTag("new_chat_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = ElectricBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VoidDark,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = VoidDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundGradient)
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Messages or Empty Starter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (messages.isEmpty() && !isLoading) {
                            StarterSuggestions(
                                onSelectSuggestion = { suggestion ->
                                    viewModel.sendMessage(suggestion)
                                }
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    val isLatest = msg.role == "model" && msg.id == messages.lastOrNull { it.role == "model" }?.id
                                    val isCurrentlySpeaking = speakingMessageId == msg.id

                                    MessageBubble(
                                        message = msg,
                                        isSpeaking = isCurrentlySpeaking,
                                        isLatestAssistant = isLatest,
                                        onSpeakClick = {
                                            viewModel.speakMessage(msg.id, msg.content)
                                        },
                                        onCopyClick = {
                                            viewModel.copyToClipboard(context, msg.content)
                                        },
                                        onRegenerateClick = {
                                            viewModel.regenerateLastResponse()
                                        }
                                    )
                                }

                                if (isLoading) {
                                    item {
                                        ThinkingIndicator()
                                    }
                                }
                            }
                        }
                    }

                    // Input Bar with Attachment Preview and Voice input
                    ChatInputBar(
                        prompt = inputPrompt,
                        attachedImageUri = attachedImageUri,
                        isListening = isListening,
                        isLoading = isLoading,
                        speechLanguage = speechLanguage,
                        onPromptChange = { viewModel.inputPrompt.value = it },
                        onRemoveImage = { viewModel.attachedImageUri.value = null },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onMicClick = {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onToggleSpeechLanguage = {
                            viewModel.speechLanguage.value = if (speechLanguage == SpeechLanguage.HINDI) {
                                SpeechLanguage.ENGLISH
                            } else {
                                SpeechLanguage.HINDI
                            }
                        },
                        onSend = {
                            viewModel.sendMessage()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    prompt: String,
    attachedImageUri: Uri?,
    isListening: Boolean,
    isLoading: Boolean,
    speechLanguage: SpeechLanguage,
    onPromptChange: (String) -> Unit,
    onRemoveImage: () -> Unit,
    onPickPhoto: () -> Unit,
    onMicClick: () -> Unit,
    onToggleSpeechLanguage: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = (prompt.isNotBlank() || attachedImageUri != null) && !isLoading

    val micInfiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by micInfiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Attached Image Preview
            AnimatedVisibility(visible = attachedImageUri != null) {
                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(68.dp)
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricPurple, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { onRemoveImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Listening status indicator
            AnimatedVisibility(visible = isListening) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    )
                    Text(
                        text = if (speechLanguage == SpeechLanguage.HINDI) "हिंदी में बोलें... (Listening in Hindi)" else "Listening in English...",
                        color = ElectricBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Input controls row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Photo Picker Button
                IconButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardDarkVariant)
                        .testTag("pick_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach photo for vision analysis",
                        tint = if (attachedImageUri != null) ElectricPurple else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Voice Language Toggle chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardDarkVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderHighlight),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSpeechLanguage() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("voice_language_toggle")
                ) {
                    Text(
                        text = speechLanguage.badge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (speechLanguage == SpeechLanguage.HINDI) ElectricPurple else ElectricBlue
                    )
                }

                // Mic Button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(42.dp)
                        .scale(if (isListening) micScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isListening) {
                                Brush.linearGradient(listOf(ElectricPurple, ElectricBlue))
                            } else {
                                Brush.linearGradient(listOf(CardDarkVariant, CardDarkVariant))
                            }
                        )
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop voice input" else "Start voice input",
                        tint = if (isListening) Color.White else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input Field
                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            text = "Ask in Hindi or English...",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("chat_input_textfield"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardDarkVariant,
                        unfocusedContainerColor = CardDarkVariant,
                        disabledContainerColor = CardDarkVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ElectricPurple,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) PurpleBlueGradient else Brush.linearGradient(
                                listOf(CardDarkVariant, CardDarkVariant)
                            )
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (canSend) Color.White else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
