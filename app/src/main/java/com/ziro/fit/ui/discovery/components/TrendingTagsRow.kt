package com.ziro.fit.ui.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.ui.theme.StrongBlue
import com.ziro.fit.ui.theme.StrongTextSecondary
import com.ziro.fit.ui.theme.TrendingTagBg
import com.ziro.fit.ui.theme.TrendingTagBorder

private val DEFAULT_TAGS = listOf("Strength", "Yoga", "HIIT", "Calisthenics", "Mobility", "Pilates")

/**
 * Horizontal scrolling row of trending search tag chips with sparkles icon + "#" prefix.
 *
 * Matches the iOS [TrendingTagsView] from ExploreComponents.swift lines 812-853.
 *
 * @param onTagClick called when a tag chip is tapped
 * @param tags optional custom tag list; defaults to the standard set
 * @param modifier optional [Modifier]
 */
@Composable
fun TrendingTagsRow(
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = DEFAULT_TAGS
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Text(
            text = "TRENDING SEARCHES",
            color = StrongTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp)
        )

        // ── Tag chips ───────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags, key = { it }) { tag ->
                TrendingTagChip(tag = tag, onClick = { onTagClick(tag) })
            }
        }
    }
}

@Composable
private fun TrendingTagChip(
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(TrendingTagBg, RoundedCornerShape(10.dp))
            .border(width = 1.dp, color = TrendingTagBorder, shape = RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = StrongBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "#$tag",
            color = StrongBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
