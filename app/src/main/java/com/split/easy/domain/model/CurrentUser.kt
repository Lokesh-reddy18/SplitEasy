package com.split.easy.domain.model

/** The person using the app. Until sign-in arrives in M4 this is a fixed local user (M2-Q1). */
data class CurrentUser(val userId: String, val displayName: String)
