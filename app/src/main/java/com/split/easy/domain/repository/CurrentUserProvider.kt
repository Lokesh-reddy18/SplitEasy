package com.split.easy.domain.repository

import com.split.easy.domain.model.CurrentUser

/** Who is using the app. In M2 a fixed local user; in M4 the signed-in Google account. */
interface CurrentUserProvider {
    fun currentUser(): CurrentUser
}
