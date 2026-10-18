package com.split.easy.domain.model

/** Whether someone is signed in, and whether the data on this phone is theirs yet. */
sealed interface Session {
    data object SignedOut : Session

    /**
     * Signed in, but the data on this phone belongs to nobody or to another account (M2 test data,
     * or a previous user). It's cleared before any screen shows it (M4-Q2).
     */
    data class Preparing(val user: CurrentUser) : Session

    data class SignedIn(val user: CurrentUser) : Session
}
