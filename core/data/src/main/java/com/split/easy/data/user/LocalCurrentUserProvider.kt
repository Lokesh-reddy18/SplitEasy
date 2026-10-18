package com.split.easy.data.user

import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.repository.CurrentUserProvider

/**
 * The stand-in user for M2 (M2-Q1). Everything created before sign-in belongs to this id;
 * M4 wipes that test data at the first real sign-in.
 */
class LocalCurrentUserProvider : CurrentUserProvider {
    override fun currentUser() =
        CurrentUser(userId = LOCAL_USER_ID, displayName = "You", isPlaceholder = true)

    companion object {
        const val LOCAL_USER_ID = "local-user"
    }
}
