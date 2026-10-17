package com.lokeshreddy.spliteasy.domain.group

/** Whether a group action is allowed, and if not, what the UI should say. */
sealed interface GroupCheck {
    data object Allowed : GroupCheck

    /** The user doesn't have permission (hide or disable the action). */
    data object NotAllowed : GroupCheck

    /** The group is archived, so nothing can change. */
    data object ReadOnly : GroupCheck

    /** The creator can't leave; they archive the group instead. */
    data object CreatorMustArchive : GroupCheck

    /** "Resolve pending payments first" (M1-Q7). */
    data object ResolvePendingFirst : GroupCheck

    /** "Settle ₹X first" (Q13). */
    data class SettleFirst(val outstandingPaise: Long) : GroupCheck
}
