package com.split.easy.data.user

import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.repository.AuthRepository
import com.split.easy.domain.repository.CurrentUserProvider
import javax.inject.Inject

/**
 * The signed-in user. Screens that need a user only appear after sign-in, so the stand-in below is
 * only seen for a moment while signing out; it matches no group, so nothing leaks.
 */
class FirebaseCurrentUserProvider @Inject constructor(private val auth: AuthRepository) :
    CurrentUserProvider {
    override fun currentUser(): CurrentUser = auth.currentUser() ?: SIGNED_OUT

    private companion object {
        val SIGNED_OUT =
            CurrentUser(userId = "signed-out", displayName = "You", isPlaceholder = true)
    }
}
