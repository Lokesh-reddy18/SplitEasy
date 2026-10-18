package com.split.easy.ui.expense.editor

import com.split.easy.domain.expense.ExpenseCheck
import com.split.easy.domain.expense.ExpenseRules
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.money.ParsedAmount
import com.split.easy.domain.money.formatPlainAmount
import com.split.easy.domain.money.parseInrToPaise
import com.split.easy.domain.split.FULL_PERCENT_BASIS_POINTS
import com.split.easy.domain.split.SplitCalculator
import com.split.easy.domain.split.SplitError
import com.split.easy.domain.split.SplitResult
import com.split.easy.domain.split.formatBasisPoints
import com.split.easy.domain.split.parsePercentToBasisPoints
import java.time.LocalDate

/** What's typed on the Add or Edit expense screen (04). All money is still text here. */
data class ExpenseForm(
    val amountText: String = "",
    val description: String = "",
    val paidByMemberId: String,
    val category: Category = Category.OTHER,
    val date: LocalDate,
    val splitType: SplitType = SplitType.EQUAL,
    /** Who's ticked for an equal split. */
    val includedMemberIds: Set<String>,
    /** Typed rupee amounts per member, for an exact split. */
    val exactTexts: Map<String, String> = emptyMap(),
    /** Typed percentages per member, for a percentage split. */
    val percentTexts: Map<String, String> = emptyMap(),
)

enum class AmountError {
    MISSING,
    INVALID,
    TOO_MANY_DECIMALS,
    NOT_POSITIVE,
    ABOVE_LIMIT,
}

/** The footer line: "₹2,400 of ₹2,400 assigned", "₹500 left to assign", "0.01% left to assign"… */
sealed interface SplitStatus {
    data object NeedsAmount : SplitStatus

    data object NoParticipants : SplitStatus

    /** An exact amount or percentage that isn't a valid number. */
    data object InvalidEntry : SplitStatus

    data class Balanced(val totalPaise: Long, val participants: Int) : SplitStatus

    data class ExactLeft(val paise: Long) : SplitStatus

    data class ExactOver(val paise: Long) : SplitStatus

    data class PercentLeft(val basisPoints: Int) : SplitStatus

    data class PercentOver(val basisPoints: Int) : SplitStatus
}

data class FormResult(
    val totalPaise: Long?,
    val amountError: AmountError?,
    val isDescriptionMissing: Boolean,
    val isDateTooFarAhead: Boolean,
    /** Each participant's share once the split works out; shown on the rows. */
    val sharesPaise: Map<String, Long>,
    val percentBasisPoints: Map<String, Int>,
    val splitStatus: SplitStatus,
) {
    val isValid: Boolean
        get() = totalPaise != null && amountError == null && !isDescriptionMissing &&
            !isDateTooFarAhead &&
            splitStatus is SplitStatus.Balanced
}

/**
 * Checks the form and works out the shares with the tested [SplitCalculator].
 * [memberOrder] is every selectable member in member order, so rounding is deterministic (Q7).
 */
fun ExpenseForm.evaluate(memberOrder: List<String>, today: LocalDate): FormResult {
    val (total, amountError, dateTooFar) = checkAmountAndDate(today)
    val descriptionMissing = description.isBlank()
    fun result(status: SplitStatus) = FormResult(
        total,
        amountError,
        descriptionMissing,
        dateTooFar,
        emptyMap(),
        emptyMap(),
        status,
    )

    if (total == null || amountError != null) return result(SplitStatus.NeedsAmount)

    return when (splitType) {
        SplitType.EQUAL -> {
            val participants = memberOrder.filter { it in includedMemberIds }
            fromSplit(SplitCalculator.equal(total, participants, paidByMemberId), total, dateTooFar)
        }

        SplitType.EXACT -> {
            val amounts = typedValues(exactTexts, memberOrder, ::parsePaise)
                ?: return result(SplitStatus.InvalidEntry)
            val positive = amounts.filterValues { it > 0 }
            if (positive.isEmpty()) return result(SplitStatus.ExactLeft(total))
            val over = amounts.values.sum() - total
            fromSplit(SplitCalculator.exact(total, positive), total, dateTooFar, exactOver = over)
        }

        SplitType.PERCENT -> {
            val percents = typedValues(percentTexts, memberOrder, ::parsePercentToBasisPoints)
                ?: return result(SplitStatus.InvalidEntry)
            val positive = percents.filterValues { it > 0 }
            if (positive.isEmpty()) {
                return result(SplitStatus.PercentLeft(FULL_PERCENT_BASIS_POINTS))
            }
            fromSplit(SplitCalculator.percent(total, positive, paidByMemberId), total, dateTooFar)
                .copy(percentBasisPoints = positive)
        }
    }
}

private fun parsePaise(text: String): Long? = (parseInrToPaise(text) as? ParsedAmount.Valid)?.paise

/** The non-blank entries in member order, parsed; null if any entry isn't a valid number. */
private fun <T : Any> typedValues(
    texts: Map<String, String>,
    memberOrder: List<String>,
    parse: (String) -> T?,
): Map<String, T>? {
    val typed = memberOrder.mapNotNull { id ->
        texts[id]?.takeIf { it.isNotBlank() }?.let { id to it }
    }
    val parsed = typed.map { (id, text) -> id to parse(text) }
    if (parsed.any { it.second == null }) return null
    return parsed.associate { (id, value) -> id to value!! }
}

/** Turns the calculator's answer into shares or a footer status. */
private fun ExpenseForm.fromSplit(
    split: SplitResult,
    total: Long,
    dateTooFar: Boolean,
    exactOver: Long = 0,
): FormResult {
    val base = FormResult(
        totalPaise = total,
        amountError = null,
        isDescriptionMissing = description.isBlank(),
        isDateTooFarAhead = dateTooFar,
        sharesPaise = emptyMap(),
        percentBasisPoints = emptyMap(),
        splitStatus = SplitStatus.NeedsAmount,
    )
    return when (split) {
        is SplitResult.Success -> base.copy(
            sharesPaise = split.sharesPaise,
            splitStatus = SplitStatus.Balanced(total, split.sharesPaise.size),
        )

        is SplitResult.Invalid -> base.copy(splitStatus = split.error.toStatus(exactOver))
    }
}

private fun ExpenseForm.checkAmountAndDate(today: LocalDate): Triple<Long?, AmountError?, Boolean> {
    val paise = when (val parsed = parseInrToPaise(amountText)) {
        ParsedAmount.Empty -> return Triple(null, AmountError.MISSING, false)
        ParsedAmount.Invalid -> return Triple(null, AmountError.INVALID, false)
        ParsedAmount.TooManyDecimals -> return Triple(null, AmountError.TOO_MANY_DECIMALS, false)
        is ParsedAmount.Valid -> parsed.paise
    }
    return when (ExpenseRules.check(paise, date, today)) {
        ExpenseCheck.OK -> Triple(paise, null, false)
        ExpenseCheck.AMOUNT_NOT_POSITIVE -> Triple(null, AmountError.NOT_POSITIVE, false)
        ExpenseCheck.AMOUNT_ABOVE_LIMIT -> Triple(null, AmountError.ABOVE_LIMIT, false)
        ExpenseCheck.DATE_TOO_FAR_AHEAD -> Triple(paise, null, true)
    }
}

private fun SplitError.toStatus(exactOver: Long): SplitStatus = when (this) {
    SplitError.NoParticipants -> SplitStatus.NoParticipants

    is SplitError.ExactSumMismatch -> when {
        differencePaise > 0 -> SplitStatus.ExactLeft(differencePaise)
        else -> SplitStatus.ExactOver(-differencePaise)
    }

    SplitError.ShareAboveTotal -> SplitStatus.ExactOver(exactOver)

    is SplitError.PercentSumMismatch -> when {
        differenceBasisPoints > 0 -> SplitStatus.PercentLeft(differenceBasisPoints)
        else -> SplitStatus.PercentOver(-differenceBasisPoints)
    }

    // Amount problems are caught before splitting, and the parsers never produce negatives.
    SplitError.TotalNotPositive,
    SplitError.TotalAboveLimit,
    SplitError.NegativeShare,
    SplitError.DuplicateParticipant,
    -> SplitStatus.InvalidEntry
}

/** The values to save, once [result] is valid. */
fun ExpenseForm.toDraft(groupId: String, result: FormResult): ExpenseDraft {
    check(result.isValid) { "Only a valid form can be saved" }
    return ExpenseDraft(
        groupId = groupId,
        paidByMemberId = paidByMemberId,
        amountPaise = result.totalPaise!!,
        description = description.trim(),
        category = category,
        date = date,
        splitType = splitType,
        sharesPaise = result.sharesPaise,
        percentBasisPoints = if (splitType ==
            SplitType.PERCENT
        ) {
            result.percentBasisPoints
        } else {
            emptyMap()
        },
    )
}

/** A new expense: paid by you, Other, today, split equally between everyone (M2-Q8). */
fun newExpenseForm(myMemberId: String, currentMemberIds: List<String>, today: LocalDate) =
    ExpenseForm(
        paidByMemberId = myMemberId,
        date = today,
        includedMemberIds = currentMemberIds.toSet(),
    )

/** An existing expense, back as editable text. */
fun Expense.toForm() = ExpenseForm(
    amountText = formatPlainAmount(amountPaise),
    description = description,
    paidByMemberId = paidByMemberId,
    category = category,
    date = date,
    splitType = splitType,
    includedMemberIds = sharesPaise.keys,
    exactTexts = if (splitType ==
        SplitType.EXACT
    ) {
        sharesPaise.mapValues { formatPlainAmount(it.value) }
    } else {
        emptyMap()
    },
    percentTexts = percentBasisPoints.mapValues { formatBasisPoints(it.value) },
)
