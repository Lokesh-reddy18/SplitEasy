package com.split.easy.ui.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.split.easy.config.FeatureFlags
import com.split.easy.core.designsystem.R
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.money.formatInr
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.CategoryIcon
import com.split.easy.ui.components.ChipStyle
import com.split.easy.ui.components.StatusChip
import com.split.easy.ui.theme.SplitEasyTheme

private val RowPadding = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)

/** "Dinner at Raintree · Priya paid ₹2,400 · you borrowed ₹480" (03). */
@Composable
fun ExpenseRow(item: FeedItem.ExpenseItem, onClick: () -> Unit) {
    val colors = SplitEasyTheme.colors
    val payer = item.payerName ?: stringResource(R.string.you)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(item.expense.category)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (FeatureFlags.SHOW_SYNC_STATUS && !item.expense.isSynced) {
                    StatusChip(stringResource(R.string.feed_not_synced), ChipStyle.OWE)
                }
            }
            Text(
                stringResource(R.string.feed_paid, payer, formatInr(item.expense.amountPaise)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        ImpactLabel(item.impact)
    }
}

@Composable
private fun ImpactLabel(impact: ExpenseImpact) {
    val colors = SplitEasyTheme.colors
    when (impact) {
        is ExpenseImpact.Lent -> RightAmount(
            stringResource(R.string.feed_you_lent),
            impact.paise,
            isOwed = true,
        )

        is ExpenseImpact.Borrowed -> RightAmount(
            stringResource(R.string.feed_you_borrowed),
            impact.paise,
            isOwed = false,
        )

        ExpenseImpact.NotInvolved -> Text(
            stringResource(R.string.feed_not_involved),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
        )

        ExpenseImpact.NoBalance -> Unit
    }
}

@Composable
private fun RightAmount(label: String, paise: Long, isOwed: Boolean) {
    val colors = SplitEasyTheme.colors
    Column(horizontalAlignment = Alignment.End) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.muted,
        )
        AmountText(
            paise,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isOwed) colors.primaryText else colors.owe,
        )
    }
}

/** A settlement in the feed: "Ravi paid you · via UPI · waiting for you", with a Pending chip (18). */
@Composable
fun SettlementRow(item: FeedItem.SettlementItem, onClick: (() -> Unit)?) {
    val colors = SplitEasyTheme.colors
    val settlement = item.settlement
    val title = settlementTitle(item)
    val method = stringResource(
        if (settlement.method ==
            SettlementMethod.UPI
        ) {
            R.string.feed_via_upi
        } else {
            R.string.feed_in_cash
        },
    )
    val subtitle = when (settlement.status) {
        SettlementStatus.PENDING ->
            item.toName?.let { stringResource(R.string.feed_waiting_for, method, it) }
                ?: stringResource(R.string.feed_waiting_for_you, method)

        SettlementStatus.REJECTED -> stringResource(R.string.feed_not_received, method)

        SettlementStatus.CONFIRMED -> method
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 72.dp)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(colors.primaryTint, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_swap),
                null,
                tint = colors.onPrimaryTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (settlement.status == SettlementStatus.PENDING) {
                StatusChip(stringResource(R.string.feed_pending), ChipStyle.PRIMARY)
            }
            AmountText(
                settlement.amountPaise,
                style = MaterialTheme.typography.bodyLarge,
                color = if (settlement.status ==
                    SettlementStatus.REJECTED
                ) {
                    colors.muted
                } else {
                    colors.ink
                },
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun settlementTitle(item: FeedItem.SettlementItem): String = when {
    item.fromName == null -> stringResource(
        R.string.feed_settlement_you_paid,
        item.toName.orEmpty(),
    )

    item.toName == null -> stringResource(R.string.feed_settlement_paid_you, item.fromName)

    else -> stringResource(R.string.feed_settlement_paid, item.fromName, item.toName)
}

/** "Arjun set Ravi's UPI ID to ravi@okhdfc", with the new ID in bold (18, H5). */
@Composable
fun EventRow(item: FeedItem.EventItem) {
    val colors = SplitEasyTheme.colors
    val actor = item.actorName ?: stringResource(R.string.you)
    val event = item.event
    val newValue = event.newValue
    val oldValue = event.oldValue
    val sentence = if (oldValue == null) {
        stringResource(R.string.feed_event_upi_set, actor, item.targetName, newValue)
    } else {
        stringResource(
            R.string.feed_event_upi_changed,
            actor,
            item.targetName,
            oldValue,
            newValue,
        )
    }
    val text = buildAnnotatedString {
        val start = sentence.lastIndexOf(newValue)
        append(sentence.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.ink)) { append(newValue) }
        append(sentence.substring(start + newValue.length))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_edit),
                null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onNeutralTint,
            modifier = Modifier.weight(1f),
        )
    }
}
