package com.split.easy.domain.model

/** The signed-in person (their Google account). */
data class CurrentUser(
    val userId: String,
    /** Their Google name; screens still say "You" for them. */
    val displayName: String,
    val email: String? = null,
    /** True only for the stand-in returned while nobody is signed in; no screen shows it. */
    val isPlaceholder: Boolean = false,
)
