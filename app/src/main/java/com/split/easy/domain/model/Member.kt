package com.split.easy.domain.model

/** A person in a group. Guests have no account ([userId] is null) and never use the app. */
data class Member(val id: String, val userId: String?, val isRemoved: Boolean = false) {
    val isGuest: Boolean
        get() = userId == null
}
