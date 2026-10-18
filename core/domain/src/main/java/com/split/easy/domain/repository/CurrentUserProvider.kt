package com.split.easy.domain.repository

import com.split.easy.domain.model.CurrentUser

/** Who is using the app: the signed-in Google account. */
interface CurrentUserProvider {
    fun currentUser(): CurrentUser
}
