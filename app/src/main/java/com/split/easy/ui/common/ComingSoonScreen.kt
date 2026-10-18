package com.split.easy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.split.easy.R
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.TopBarNavigation
import com.split.easy.ui.theme.SplitEasyTheme

/** Temporary stand-in for screens built later in Milestone 2 (group detail, add expense, profile). */
@Composable
fun ComingSoonScreen(title: String, onBack: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SplitEasyTheme.colors.background),
    ) {
        SplitEasyTopBar(
            title = title,
            navigation = if (onBack != null) TopBarNavigation.BACK else null,
            onNavigate = { onBack?.invoke() },
        )
        Text(
            text = stringResource(R.string.coming_soon_message),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = SplitEasyTheme.colors.muted,
        )
    }
}
