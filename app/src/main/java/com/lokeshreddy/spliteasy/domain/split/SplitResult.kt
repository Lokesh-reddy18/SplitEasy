package com.lokeshreddy.spliteasy.domain.split

sealed interface SplitResult {
    /** Each participant's share in paise, in member order. The shares always add up to the total. */
    data class Success(val sharesPaise: Map<String, Long>) : SplitResult

    data class Invalid(val error: SplitError) : SplitResult
}
