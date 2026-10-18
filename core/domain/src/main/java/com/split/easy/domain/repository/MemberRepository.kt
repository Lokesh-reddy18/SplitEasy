package com.split.easy.domain.repository

import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    /** Everyone who was ever in the group, in member order. Removed members have isRemoved = true. */
    fun observeMembers(groupId: String): Flow<List<Member>>

    /** Adds a guest tracked by name. Returns the new member's id. */
    suspend fun addGuest(groupId: String, displayName: String): String

    /** Removes a member, or leaves the group when it's the current user. Their history stays. */
    suspend fun removeMember(memberId: String)

    /**
     * Sets a guest's UPI ID and records a group event in the same transaction (F1, F10),
     * so the change always shows in the feed.
     */
    suspend fun setGuestUpiId(guestMemberId: String, actorMemberId: String, upiId: String)

    /** Sets the current user's UPI ID on their member row in every group (F2). */
    suspend fun setMyUpiId(upiId: String)

    fun observeEvents(groupId: String): Flow<List<GroupEvent>>
}
