package com.ziro.fit.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.VoiceCoachAgentState
import com.ziro.fit.model.VoiceCoachConnectionState
import com.ziro.fit.model.VoiceMessage
import com.ziro.fit.model.MessageRole
import com.ziro.fit.ui.theme.StrongBackground
import com.ziro.fit.ui.theme.StrongBlue
import com.ziro.fit.ui.theme.StrongGreen
import com.ziro.fit.ui.theme.StrongRed
import com.ziro.fit.ui.theme.StrongSecondaryBackground
import com.ziro.fit.ui.theme.StrongTextPrimary
import com.ziro.fit.ui.theme.StrongTextSecondary
import kotlinx.coroutines.launch

/**
 * Bottom overlay panel that appears when the voice coach session is active.
 *
 * Mirrors the iOS voice training screen with:
 * - Agent state indicator (listening / thinking / speaking)
 * - Waveform visualization based on audio level
 * - Conversation transcript (scrollable)
 * - Close button to disconnect
 */
@Composable
fun VoiceCoachOverlay(
    visible: Boolean,
    connectionState: VoiceCoachConnectionState,
    agentState: VoiceCoachAgentState,
    audioLevel: Float,
    messages: List<VoiceMessage>,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(StrongSecondaryBackground)
                .padding(bottom = 32.dp)
        ) {
            // ── Drag Handle ──
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            // ── Header Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AI Voice Coach",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StrongTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    AgentStatusChip(agentState = agentState)
                }

                IconButton(onClick = onDisconnect) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = StrongTextSecondary
                    )
                }
            }

            // ── Waveform Visualization ──
            WaveformBar(
                audioLevel = audioLevel,
                agentState = agentState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Transcript ──
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // Auto-scroll to latest message
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Speak to start your voice-guided workout...",
                                fontSize = 14.sp,
                                color = StrongTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        TranscriptBubble(message = message)
                    }
                }
            }

            // ── Connection Status ──
            if (connectionState == VoiceCoachConnectionState.ERROR) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StrongRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Connection lost. Tap 'Try Again' to reconnect.",
                        fontSize = 13.sp,
                        color = StrongRed
                    )
                }
            }

            // ── Mic indicator ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                MicIndicator(
                    isActive = connectionState == VoiceCoachConnectionState.CONNECTED,
                    audioLevel = audioLevel
                )
            }
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────

@Composable
private fun AgentStatusChip(agentState: VoiceCoachAgentState) {
    val (label, color) = when (agentState) {
        VoiceCoachAgentState.LISTENING -> "Listening" to StrongGreen
        VoiceCoachAgentState.THINKING -> "Thinking..." to Color(0xFFFFA500)
        VoiceCoachAgentState.SPEAKING -> "Speaking" to StrongBlue
        VoiceCoachAgentState.UNKNOWN -> "Idle" to StrongTextSecondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Pulsing dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = StrongTextSecondary
        )
    }
}

@Composable
private fun WaveformBar(
    audioLevel: Float,
    agentState: VoiceCoachAgentState,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = audioLevel,
        animationSpec = tween(150, easing = LinearEasing),
        label = "waveformLevel"
    )

    val barCount = 40
    val color = when (agentState) {
        VoiceCoachAgentState.LISTENING -> StrongGreen
        VoiceCoachAgentState.THINKING -> Color(0xFFFFA500)
        VoiceCoachAgentState.SPEAKING -> StrongBlue
        VoiceCoachAgentState.UNKNOWN -> StrongTextSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            // Distribute the audio level across bars with some variation
            val barHeight = if (animatedLevel > 0.01f) {
                val peak = (kotlin.math.sin(i * 0.5f) * 0.5f + 0.5f) * animatedLevel * 0.8f + animatedLevel * 0.2f
                peak.coerceIn(0.05f, 1f)
            } else {
                0.08f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((barHeight * 32).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color.copy(alpha = barHeight.coerceIn(0.3f, 1f)))
            )
        }
    }
}

@Composable
private fun TranscriptBubble(message: VoiceMessage) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) StrongBlue.copy(alpha = 0.15f) else StrongBackground
    val textColor = StrongTextPrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = message.content,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 14.sp,
            color = textColor,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun MicIndicator(
    isActive: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1f + audioLevel * 0.3f else 1f,
        animationSpec = tween(100),
        label = "micScale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(animatedScale)
            .clip(CircleShape)
            .background(if (isActive) StrongBlue.copy(alpha = 0.2f) else StrongBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isActive) "Microphone active" else "Microphone inactive",
            tint = if (isActive) StrongBlue else StrongTextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

