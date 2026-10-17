package com.split.easy.domain.split

/** Why a split can't be saved. The UI turns each one into a message. */
sealed interface SplitError {
    data object TotalNotPositive : SplitError

    data object TotalAboveLimit : SplitError

    data object NoParticipants : SplitError

    data object DuplicateParticipant : SplitError

    data object NegativeShare : SplitError

    /** One exact amount is bigger than the whole expense. */
    data object ShareAboveTotal : SplitError

    /** Total minus the exact amounts: positive means "₹X left to assign", negative means over by ₹X. */
    data class ExactSumMismatch(val differencePaise: Long) : SplitError

    /** 100.00% minus the entered percentages, in basis points (1 bp = 0.01%). */
    data class PercentSumMismatch(val differenceBasisPoints: Int) : SplitError
}
