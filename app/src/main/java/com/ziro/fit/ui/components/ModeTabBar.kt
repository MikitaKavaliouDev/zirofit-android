package com.ziro.fit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.AppMode
import com.ziro.fit.ui.theme.ZiroAccent
import com.ziro.fit.util.HapticNotification
import com.ziro.fit.util.HapticStyle

enum class TabItem(
    val trainerLabel: String,
    val personalLabel: String,
    val trainerIcon: ImageVector,
    val personalIcon: ImageVector
) {
    CALENDAR("Calendar", "Calendar", Icons.Default.CalendarMonth, Icons.Default.CalendarMonth),
    PROGRAMS("Programs", "Explore", Icons.Default.Search, Icons.Default.Search),
    HOME("Home", "Home", Icons.Default.Home, Icons.Default.Home),
    CLIENTS("Clients", "Workouts", Icons.Default.People, Icons.AutoMirrored.Filled.List),
    MORE("More", "More", Icons.Default.Menu, Icons.Default.Menu),
    ANALYTICS("Analytics", "Analytics", Icons.Default.BarChart, Icons.Default.BarChart);
    
    fun label(mode: AppMode) = if (mode == AppMode.TRAINER) trainerLabel else personalLabel
    fun icon(mode: AppMode) = if (mode == AppMode.TRAINER) trainerIcon else personalIcon

    companion object {
        fun trainerTabs() = listOf(CALENDAR, PROGRAMS, HOME, CLIENTS, MORE)
        fun personalTabs() = listOf(PROGRAMS, CLIENTS, HOME, ANALYTICS, MORE)
        fun tabsFor(mode: AppMode) = if (mode == AppMode.TRAINER) trainerTabs() else personalTabs()
    }
}

@Composable
fun ModeTabBar(
    currentMode: AppMode,
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = TabItem.tabsFor(currentMode)
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    val icon = tab.icon(currentMode)
                    val label = tab.label(currentMode)
                    val isSelected = tab == selectedTab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            HapticManagerCompat.impact(HapticStyle.LIGHT)
                            onTabSelected(tab)
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ZiroAccent,
                            selectedTextColor = ZiroAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}

object HapticManagerCompat {
    fun impact(style: HapticStyle) {
        try {
            com.ziro.fit.ZiroFitApp.globalHapticManager?.impact(style)
        } catch (_: Exception) { }
    }
    fun notification(type: HapticNotification) {
        try {
            com.ziro.fit.ZiroFitApp.globalHapticManager?.notification(type)
        } catch (_: Exception) { }
    }
}
