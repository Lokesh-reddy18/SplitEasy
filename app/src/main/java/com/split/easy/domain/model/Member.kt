package com.split.easy.domain.model

/** A person in a group. Guests have no account ([userId] is null) and never use the app. */
data class Member(
    val id: String,
    val displayName: String,
    val userId: String?,
    val upiId: String? = null,
    /** Join time; member order is join time, then id (M1-Q2). */
    val createdAt: Long = 0L,
    val isRemoved: Boolean = false,
) {
    val isGuest: Boolean
        get() = userId == null
}
