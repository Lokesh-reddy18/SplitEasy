package com.split.easy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.ui.theme.SplitEasyTheme

/** A catalogue of the shared pieces, for checking them in Android Studio's preview (light and dark). */
@Composable
private fun ComponentsCatalogue() {
    val colors = SplitEasyTheme.colors
    Column(
        modifier = Modifier
            .background(colors.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SplitEasyTopBar(title = "Coorg trip", subtitle = "You, Priya, Meera, Ravi, Arjun")
        SectionHeader("Sat, 10 Oct")
        SplitEasyCard {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIcon(Category.FOOD)
                Column(
                    Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text("Dinner at Raintree", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Priya paid ₹2,400",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                AmountText(48_000, color = colors.owe)
            }
            CardDivider()
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar("Ravi Kumar")
                Spacer(Modifier.padding(6.dp))
                StatusChip("Pending", ChipStyle.PRIMARY)
                Spacer(Modifier.weight(1f))
                AmountText(-1_70_000, signed = true, color = colors.owe)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Settle up", onClick = {}, modifier = Modifier.weight(1f))
            SecondaryButton("Balances", onClick = {}, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedActionButton("Not received", onClick = {}, modifier = Modifier.weight(1f))
            DestructiveButton("Delete", onClick = {}, modifier = Modifier.weight(1f))
        }
        TextActionButton("New group", onClick = {}, icon = R.drawable.ic_add)
    }
}

@Preview(name = "Light", widthDp = 390)
@Composable
private fun ComponentsLightPreview() {
    SplitEasyTheme(ThemeSetting.LIGHT) { ComponentsCatalogue() }
}

@Preview(name = "Dark", widthDp = 390)
@Composable
private fun ComponentsDarkPreview() {
    SplitEasyTheme(ThemeSetting.DARK) { ComponentsCatalogue() }
}
