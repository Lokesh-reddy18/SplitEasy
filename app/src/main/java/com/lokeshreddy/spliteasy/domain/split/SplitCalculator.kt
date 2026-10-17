package com.lokeshreddy.spliteasy.domain.split

import com.lokeshreddy.spliteasy.domain.money.MAX_AMOUNT_PAISE

/** 100.00% in basis points. Percentages are stored with two decimals: 33.33% = 3333. */
const val FULL_PERCENT_BASIS_POINTS = 10_000

/**
 * Divides an expense between members. All money is whole paise ([Long]).
 *
 * Rounding rule (FR-5): any paise left over after dividing go to the payer's share.
 * If the payer isn't part of the split, they go to the first participant in member order.
 * Inputs must be given in member order so every device gets the same result.
 */
object SplitCalculator {

    /** Splits [totalPaise] equally between [participantIds]. */
    fun equal(totalPaise: Long, participantIds: List<String>, payerId: String): SplitResult {
        validateTotal(totalPaise)?.let { return SplitResult.Invalid(it) }
        if (participantIds.isEmpty()) return SplitResult.Invalid(SplitError.NoParticipants)
        if (participantIds.toSet().size != participantIds.size) {
            return SplitResult.Invalid(SplitError.DuplicateParticipant)
        }

        val baseShare = totalPaise / participantIds.size
        val shares = participantIds.associateWith { baseShare }
        return SplitResult.Success(giveRemainder(shares, totalPaise, payerId))
    }

    /**
     * Splits [totalPaise] by percentage. [basisPointsByMember] must add up to exactly 100.00%.
     * Members with 0% are kept with a zero share but aren't counted as participants.
     */
    fun percent(
        totalPaise: Long,
        basisPointsByMember: Map<String, Int>,
        payerId: String,
    ): SplitResult {
        validateTotal(totalPaise)?.let { return SplitResult.Invalid(it) }
        if (basisPointsByMember.isEmpty()) return SplitResult.Invalid(SplitError.NoParticipants)
        if (basisPointsByMember.values.any { it < 0 }) {
            return SplitResult.Invalid(SplitError.NegativeShare)
        }
        val totalBasisPoints = basisPointsByMember.values.sum()
        if (totalBasisPoints != FULL_PERCENT_BASIS_POINTS) {
            val difference = FULL_PERCENT_BASIS_POINTS - totalBasisPoints
            return SplitResult.Invalid(SplitError.PercentSumMismatch(difference))
        }

        // Round each share down; what's left over goes to one person below.
        val shares = basisPointsByMember.mapValues { (_, basisPoints) ->
            totalPaise * basisPoints / FULL_PERCENT_BASIS_POINTS
        }
        val participants = basisPointsByMember.filterValues { it > 0 }.keys
        return SplitResult.Success(giveRemainder(shares, totalPaise, payerId, participants))
    }

    /** Uses the exact amounts given. They must add up to [totalPaise]; nothing is rounded. */
    fun exact(totalPaise: Long, amountsByMember: Map<String, Long>): SplitResult {
        validateTotal(totalPaise)?.let { return SplitResult.Invalid(it) }
        if (amountsByMember.isEmpty()) return SplitResult.Invalid(SplitError.NoParticipants)
        if (amountsByMember.values.any { it < 0 }) {
            return SplitResult.Invalid(SplitError.NegativeShare)
        }
        if (amountsByMember.values.any { it > totalPaise }) {
            return SplitResult.Invalid(SplitError.ShareAboveTotal)
        }
        // Each amount is at most the total, so this sum can't overflow.
        val assigned = amountsByMember.values.sum()
        if (assigned != totalPaise) {
            return SplitResult.Invalid(SplitError.ExactSumMismatch(totalPaise - assigned))
        }
        return SplitResult.Success(amountsByMember)
    }

    private fun validateTotal(totalPaise: Long): SplitError? = when {
        totalPaise <= 0 -> SplitError.TotalNotPositive
        totalPaise > MAX_AMOUNT_PAISE -> SplitError.TotalAboveLimit
        else -> null
    }

    /**
     * Adds the paise lost to rounding to one share: the payer's if they're a participant,
     * otherwise the first participant's.
     */
    private fun giveRemainder(
        shares: Map<String, Long>,
        totalPaise: Long,
        payerId: String,
        participants: Set<String> = shares.keys,
    ): Map<String, Long> {
        val remainder = totalPaise - shares.values.sum()
        if (remainder == 0L) return shares
        val receiver = if (payerId in participants) payerId else participants.first()
        return shares.mapValues { (memberId, share) ->
            if (memberId == receiver) share + remainder else share
        }
    }
}
