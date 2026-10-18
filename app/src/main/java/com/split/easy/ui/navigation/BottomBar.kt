package com.split.easy.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.ui.theme.SplitEasyTheme

/** The two tabs in M2. Insights joins in M5 (M2-Q2). */
enum class TopLevelTab(@param:DrawableRes val icon: Int, @param:StringRes val label: Int) {
    GROUPS(R.drawable.ic_nav_groups, R.string.nav_groups),
    PROFILE(R.drawable.ic_nav_profile, R.string.nav_profile),
}

@Composable
fun SplitEasyBottomBar(selected: TopLevelTab, onSelect: (TopLevelTab) -> Unit) {
    val colors = SplitEasyTheme.colors
    val border = colors.border
    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        // The designs draw a thin line above the bar instead of a shadow.
        modifier = Modifier.drawBehind {
            drawLine(border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        },
    ) {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(painterResource(tab.icon), contentDescription = null, Modifier.size(22.dp))
                },
                label = {
                    Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primaryText,
                    selectedTextColor = colors.primaryText,
                    unselectedIconColor = colors.muted,
                    unselectedTextColor = colors.muted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
