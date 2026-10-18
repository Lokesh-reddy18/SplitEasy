package com.split.easy.domain.group

import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import kotlin.math.abs

/**
 * Group permissions (F5) and the "balance must be zero" checks (Q13, M1-Q7).
 * Firestore rules can't add up balances, so these balance checks only run in the app (F11).
 */
object GroupRules {

    fun isCreator(user: Member, group: Group): Boolean = user.userId == group.createdByUserId

    /** Rename, add guests and share the invite: any current member, unless the group is archived. */
    fun checkMemberAction(user: Member, group: Group): GroupCheck = when {
        user.isGuest || user.isRemoved -> GroupCheck.NotAllowed
        group.isArchived -> GroupCheck.ReadOnly
        else -> GroupCheck.Allowed
    }

    /** Revoke the invite link: creator only. */
    fun checkRevokeInvite(user: Member, group: Group): GroupCheck = when {
        !isCreator(user, group) -> GroupCheck.NotAllowed
        group.isArchived -> GroupCheck.ReadOnly
        else -> GroupCheck.Allowed
    }

    /** Archive: creator only, once every balance is zero and nothing is pending. */
    fun checkArchive(
        user: Member,
        group: Group,
        netBalances: Map<String, Long>,
        settlements: List<Settlement>,
    ): GroupCheck {
        val outstanding = netBalances.values.filter { it > 0 }.sum()
        return when {
            !isCreator(user, group) -> GroupCheck.NotAllowed
            group.isArchived -> GroupCheck.ReadOnly
            settlements.any { it.isOpenPending } -> GroupCheck.ResolvePendingFirst
            outstanding > 0 -> GroupCheck.SettleFirst(outstanding)
            else -> GroupCheck.Allowed
        }
    }

    /** Unarchive: creator only. */
    fun checkUnarchive(user: Member, group: Group): GroupCheck = when {
        user.userId == null || !canUnarchive(user.userId, group) -> GroupCheck.NotAllowed
        else -> GroupCheck.Allowed
    }

    /** For the Archived groups list, where only the user id is known. */
    fun canUnarchive(userId: String, group: Group): Boolean =
        group.isArchived && group.createdByUserId == userId

    /** Remove a member: creator only, never the creator, and only when the member is settled. */
    fun checkRemoveMember(
        user: Member,
        group: Group,
        target: Member,
        targetNetPaise: Long,
        settlements: List<Settlement>,
    ): GroupCheck = when {
        !isCreator(user, group) -> GroupCheck.NotAllowed
        group.isArchived -> GroupCheck.ReadOnly
        target.isRemoved || isCreator(target, group) -> GroupCheck.NotAllowed
        hasPending(target, settlements) -> GroupCheck.ResolvePendingFirst
        targetNetPaise != 0L -> GroupCheck.SettleFirst(abs(targetNetPaise))
        else -> GroupCheck.Allowed
    }

    /** Leave: anyone except the creator, once their own balance is zero. */
    fun checkLeave(
        user: Member,
        group: Group,
        userNetPaise: Long,
        settlements: List<Settlement>,
    ): GroupCheck = when {
        user.isGuest || user.isRemoved -> GroupCheck.NotAllowed
        isCreator(user, group) -> GroupCheck.CreatorMustArchive
        group.isArchived -> GroupCheck.ReadOnly
        hasPending(user, settlements) -> GroupCheck.ResolvePendingFirst
        userNetPaise != 0L -> GroupCheck.SettleFirst(abs(userNetPaise))
        else -> GroupCheck.Allowed
    }

    private fun hasPending(member: Member, settlements: List<Settlement>): Boolean =
        settlements.any {
            it.isOpenPending && (it.fromMemberId == member.id || it.toMemberId == member.id)
        }
}
