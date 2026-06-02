package com.ziro.fit.ui.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.VoiceCoachConnectionState
import com.ziro.fit.ui.theme.StrongBackground
import com.ziro.fit.ui.theme.StrongBlue
import com.ziro.fit.ui.theme.StrongRed
import com.ziro.fit.ui.theme.StrongTextPrimary
import com.ziro.fit.ui.theme.StrongTextSecondary

/**
 * Floating voice coach button that mirrors the iOS design.
 *
 * Four visual states matching iOS VoiceCoachService:
 *
 *   DISCONNECTED → Blue "Voice Coach" button with mic icon
 *   CONNECTING   → Blue button with spinner + "Connecting..."
 *   CONNECTED    → Red "Disconnect Coach" button with mic-off icon
 *   ERROR        → Orange/red "Try Again" button with warning icon, pulsing
 */
@Composable
fun VoiceCoachButton(
    connectionState: VoiceCoachConnectionState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (connectionState) {
        VoiceCoachConnectionState.DISCONNECTED -> StrongBlue to "Voice Coach"
        VoiceCoachConnectionState.CONNECTING -> StrongBlue to "Connecting..."
        VoiceCoachConnectionState.CONNECTED -> StrongRed to "Disconnect Coach"
        VoiceCoachConnectionState.ERROR -> Color(0xFFFF6B35) to "Try Again"
    }

    val icon = when (connectionState) {
        VoiceCoachConnectionState.DISCONNECTED -> Icons.Default.Mic
        VoiceCoachConnectionState.CONNECTING -> Icons.Default.Mic
        VoiceCoachConnectionState.CONNECTED -> Icons.Default.MicOff
        VoiceCoachConnectionState.ERROR -> Icons.Default.Warning
    }

    // Pulsing effect for ERROR state
    val infiniteTransition = rememberInfiniteTransition(label = "voiceCoachPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scale = if (connectionState == VoiceCoachConnectionState.ERROR) pulseScale else 1f

    // Clickable — but skip during CONNECTING
    val onClick = if (connectionState == VoiceCoachConnectionState.CONNECTING) ({}) else onTap

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (connectionState == VoiceCoachConnectionState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Label
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
