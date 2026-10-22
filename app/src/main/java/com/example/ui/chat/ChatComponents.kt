package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.SearchSource
import com.example.ui.theme.AiBubbleBg
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardDarkVariant
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonBadgeGradient
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleEnd
import com.example.ui.theme.UserBubbleStart
import com.example.util.SpeechLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdAiLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NeonBadgeGradient)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "A D",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("thinking_indicator"),
        shape = RoundedCornerShape(20.dp),
        color = AiBubbleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(NeonBadgeGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "AD AI is thinking",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricPurple.copy(alpha = dot1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = dot2Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricPurple.copy(alpha = dot3Alpha))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    isLatestAssistant: Boolean,
    onSpeakClick: () -> Unit,
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.role == "user"
    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User message bubble
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth(0.88f)
            ) {
                // Attached image if any
                if (!message.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Attached photo",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorderHighlight, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    ),
                    color = Color.Transparent,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(UserBubbleStart, UserBubbleEnd)
                            )
                        )
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                Text(
                    text = timeFormatted,
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                )
            }
        } else {
            // AI message bubble
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth(0.96f)
            ) {
                // Header row with AD AI avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(NeonBadgeGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "AD AI",
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• $timeFormatted",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    color = if (message.isError) Color(0xFF2B1218) else AiBubbleBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (message.isError) Color(0xFF882333) else AiBubbleBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = message.content,
                            color = if (message.isError) Color(0xFFFFB4AB) else TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        // Web search sources if any
                        if (message.sources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sources / संदर्भ:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricPurple
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.sources.take(4).forEach { source ->
                                    SourceChip(source = source)
                                }
                            }
                        }

                        // Action toolbar
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // TTS Speak / Stop button
                            IconButton(
                                onClick = onSpeakClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("tts_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop reading" else "Listen to answer",
                                    tint = if (isSpeaking) ElectricBlue else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Copy button
                            IconButton(
                                onClick = onCopyClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("copy_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy answer",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message.content)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share AD AI Answer")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share answer",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isLatestAssistant) {
                                // Regenerate response button
                                IconButton(
                                    onClick = onRegenerateClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("regenerate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate answer",
                                        tint = ElectricPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceChip(source: SearchSource) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDarkVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = source.title,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun StarterSuggestions(
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonBadgeGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AD",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Welcome to AD AI",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Hindi & English AI Assistant",
            fontSize = 14.sp,
            color = ElectricBlue,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ask questions, explore ideas, upload photos, or speak in Hindi & English.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        val suggestions = listOf(
            "🌟 हिंदी में क्वांटम फिजिक्स सरल शब्दों में समझाएं",
            "✉️ Write a polite email to request sick leave",
            "🌐 What are the latest breakthrough updates in AI?",
            "📸 Analyze and summarize whatever is in my photo",
            "🎯 दैनिक दिनचर्या और फोकस के लिए 5 उपयोगी टिप्स"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSuggestion(suggestion) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
