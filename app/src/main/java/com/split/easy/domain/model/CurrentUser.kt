package com.split.easy.domain.model

/** The person using the app. Until sign-in arrives in M4 this is a fixed local user (M2-Q1). */
data class CurrentUser(
    val userId: String,
    val displayName: String,
    /** True for the M2 stand-in user, which has no real name to show initials for. */
    val isPlaceholder: Boolean = false,
)
