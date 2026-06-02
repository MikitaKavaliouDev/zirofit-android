package com.ziro.fit.ui.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.ui.theme.ExplorePurple
import com.ziro.fit.ui.theme.MapCardBlue
import com.ziro.fit.ui.theme.StrongBlue

/**
 * Card preview for the interactive match map showing trainer count.
 *
 * Matches the iOS [MapSpotlightPreviewCard] from ExploreComponents.swift lines 857-924.
 *
 * @param trainerCount number of trainers near the user
 * @param onOpenMap called when the card is tapped
 * @param modifier optional [Modifier]
 */
@Composable
fun MapSpotlightPreviewCard(
    trainerCount: Int,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onOpenMap
    ) {
        Box {
            // Base background: MapCardBlue with black gradient overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MapCardBlue, RoundedCornerShape(18.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
            )

            // ── Content row ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Left: gradient square with decorative circles + map icon ──
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(StrongBlue, ExplorePurple)
                            )
                        )
                ) {
                    // Overlapping decorative circles
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val circleRadius = 19.dp.toPx()
                        val strokeWidth = 1.5.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = circleRadius,
                            center = Offset(
                                x = size.width * 0.35f,
                                y = size.height * 0.4f
                            ),
                            style = Stroke(width = strokeWidth)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = circleRadius,
                            center = Offset(
                                x = size.width * 0.65f,
                                y = size.height * 0.55f
                            ),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    // Map icon on top
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // ── Center: title + subtitle ─────────────────────────────
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Interactive Match Map",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$trainerCount trainer${if (trainerCount != 1) "s" else ""} near you",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Right: chevron ───────────────────────────────────────
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open map",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
