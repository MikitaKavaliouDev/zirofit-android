package com.ziro.fit.ui.discovery.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.ui.theme.*

enum class ExploreTab(val label: String) {
    Trainers("Trainers"),
    Events("Events")
}

@Composable
fun ExploreSlidingSegment(
    selectedTab: ExploreTab,
    onTabSelected: (ExploreTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = ExploreTab.values()
    val selectedIndex = tabs.indexOf(selectedTab)
    var itemWidth by remember { mutableStateOf(0.dp) }

    val capsuleOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(CapsuleBackground)
    ) {
        // Sliding capsule indicator (beneath tab text — matchedGeometryEffect equivalent)
        if (itemWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .offset(x = capsuleOffset)
                    .width(itemWidth)
                    .clip(RoundedCornerShape(50))
                    .background(StrongSecondaryBackground)
            )
        }

        // Tab labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .onSizeChanged { size ->
                    itemWidth = (size.width / tabs.size).dp
                }
        ) {
            tabs.forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = if (selectedTab == tab) StrongBlue else StrongTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stroke border matching iOS overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, StrongTextSecondary.copy(alpha = 0.1f), RoundedCornerShape(50))
        )
    }
}
