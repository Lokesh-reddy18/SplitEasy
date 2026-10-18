package com.split.easy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.split.easy.R
import com.split.easy.ui.theme.SplitEasyTheme

/** Back arrow for pushed screens, a cross for full-screen forms (Add expense, New group). */
enum class TopBarNavigation(@param:DrawableRes val icon: Int, val labelRes: Int) {
    BACK(R.drawable.ic_back, R.string.action_back),
    CLOSE(R.drawable.ic_close, R.string.action_close),
}

/** The title row used by every pushed screen: icon, title with optional subtitle, actions. */
@Composable
fun SplitEasyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: TopBarNavigation? = TopBarNavigation.BACK,
    onNavigate: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigation != null) {
            IconButton(onClick = onNavigate) {
                Icon(
                    painter = painterResource(navigation.icon),
                    contentDescription = stringResource(navigation.labelRes),
                    tint = SplitEasyTheme.colors.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SplitEasyTheme.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitEasyTheme.colors.muted,
                )
            }
        }
        actions()
    }
}
