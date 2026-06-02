package com.ziro.fit.ui.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ziro.fit.model.TrainerSummary
import com.ziro.fit.ui.theme.ExplorePurple
import com.ziro.fit.ui.theme.PremiumButtonBg
import com.ziro.fit.ui.theme.SparkGlowPurple
import com.ziro.fit.ui.theme.SpotlightGradientEnd
import com.ziro.fit.ui.theme.SpotlightGradientStart
import com.ziro.fit.ui.theme.StrongBlue

/**
 * Hero card displaying a spotlight trainer with gradient background,
 * glow ring profile image, rating, specialties, bio, location and CTA.
 *
 * Matches the iOS [TrainerSpotlightHeroCard] design from ExploreComponents.swift lines 655-809.
 */
@Composable
fun TrainerSpotlightHeroCard(
    trainer: TrainerSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val specialties = trainer.profile?.certifications
        ?.split(",")
        ?.map { it.trim() }
        ?.take(3)
        ?.joinToString(" \u2022 ")
        ?: "Pro Trainer"

    val location = trainer.profile?.locations?.firstOrNull()?.address ?: "Online"
    val rating = trainer.profile?.averageRating ?: 5.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SpotlightGradientStart, SpotlightGradientEnd)
                    ),
                    RoundedCornerShape(22.dp)
                )
        ) {
            // Purple glow circle — blurred arc behind the avatar area
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .blur(35.dp)
                    .background(SparkGlowPurple, CircleShape)
                    .align(Alignment.TopStart)
                    .offset(x = 90.dp, y = (-45).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── Top row: Profile image + Rating ──────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile image with blue→purple gradient glow ring
                    Box(modifier = Modifier.size(72.dp)) {
                        // Outer gradient circle (the "ring")
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(StrongBlue, ExplorePurple)
                                    )
                                )
                        )
                        // Inner profile photo (2dp smaller to reveal ring)
                        AsyncImage(
                            model = trainer.profile?.profilePhotoPath,
                            contentDescription = trainer.name,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .align(Alignment.Center),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Rating capsule
                    Row(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.35f),
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", rating),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── SPOTLIGHT SPECIALIST badge ──────────────────────────
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(StrongBlue, ExplorePurple)
                            ),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SPOTLIGHT SPECIALIST",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Trainer name ────────────────────────────────────────
                Text(
                    text = trainer.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Specialty text (bullet-separated) ───────────────────
                Text(
                    text = specialties,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // ── Bio text (uses certifications fallback) ─────────────
                Text(
                    text = specialties,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Bottom row: Location + CTA ──────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // View Profile CTA button
                    Box(
                        modifier = Modifier
                            .background(PremiumButtonBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "View Profile",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
