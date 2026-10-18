package com.split.easy.domain.group

import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.testMember
import com.split.easy.domain.testSettlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupRulesTest {

    private val creator = testMember("m-creator", userId = "uid-creator")
    private val member = testMember("m-member", userId = "uid-member")
    private val guest = testMember("m-guest", userId = null)
    private val group = Group("g1", name = "Coorg trip", createdByUserId = "uid-creator")
    private val archived = group.copy(isArchived = true)

    private val settled = mapOf(creator.id to 0L, member.id to 0L, guest.id to 0L)
    private val unsettled = mapOf(creator.id to 500L, member.id to -300L, guest.id to -200L)

    private fun pending(from: Member, to: Member) =
        testSettlement(from.id, to.id, 100, SettlementStatus.PENDING, id = "s")

    @Test
    fun `creator is identified by user id`() {
        assertTrue(GroupRules.isCreator(creator, group))
        assertFalse(GroupRules.isCreator(member, group))
        assertFalse(GroupRules.isCreator(guest, group))
    }

    @Test
    fun `any current member can rename, add guests and share the invite`() {
        assertEquals(GroupCheck.Allowed, GroupRules.checkMemberAction(member, group))
        assertEquals(GroupCheck.Allowed, GroupRules.checkMemberAction(creator, group))
        assertEquals(GroupCheck.ReadOnly, GroupRules.checkMemberAction(member, archived))
        assertEquals(GroupCheck.NotAllowed, GroupRules.checkMemberAction(guest, group))
        assertEquals(
            GroupCheck.NotAllowed,
            GroupRules.checkMemberAction(member.copy(isRemoved = true), group),
        )
    }

    @Test
    fun `only the creator can revoke invites`() {
        assertEquals(GroupCheck.Allowed, GroupRules.checkRevokeInvite(creator, group))
        assertEquals(GroupCheck.NotAllowed, GroupRules.checkRevokeInvite(member, group))
        assertEquals(GroupCheck.ReadOnly, GroupRules.checkRevokeInvite(creator, archived))
    }

    @Test
    fun `archive needs the creator, zero balances and nothing pending`() {
        assertEquals(
            GroupCheck.Allowed,
            GroupRules.checkArchive(creator, group, settled, emptyList()),
        )
        assertEquals(
            GroupCheck.NotAllowed,
            GroupRules.checkArchive(member, group, settled, emptyList()),
        )
        assertEquals(
            GroupCheck.ReadOnly,
            GroupRules.checkArchive(creator, archived, settled, emptyList()),
        )
        assertEquals(
            GroupCheck.SettleFirst(500),
            GroupRules.checkArchive(creator, group, unsettled, emptyList()),
        )
        assertEquals(
            GroupCheck.ResolvePendingFirst,
            GroupRules.checkArchive(creator, group, settled, listOf(pending(member, guest))),
        )
    }

    @Test
    fun `only the creator can unarchive an archived group`() {
        assertEquals(GroupCheck.Allowed, GroupRules.checkUnarchive(creator, archived))
        assertEquals(GroupCheck.NotAllowed, GroupRules.checkUnarchive(member, archived))
        assertEquals(GroupCheck.NotAllowed, GroupRules.checkUnarchive(creator, group))
    }

    @Test
    fun `archived list checks unarchive by user id`() {
        assertTrue(GroupRules.canUnarchive("uid-creator", archived))
        assertFalse(GroupRules.canUnarchive("uid-member", archived))
        assertFalse(GroupRules.canUnarchive("uid-creator", group))
    }

    @Test
    fun `remove member needs the creator and a settled target`() {
        assertEquals(
            GroupCheck.Allowed,
            GroupRules.checkRemoveMember(creator, group, guest, 0, emptyList()),
        )
        assertEquals(
            GroupCheck.NotAllowed,
            GroupRules.checkRemoveMember(member, group, guest, 0, emptyList()),
        )
        assertEquals(
            GroupCheck.ReadOnly,
            GroupRules.checkRemoveMember(creator, archived, guest, 0, emptyList()),
        )
        assertEquals(
            GroupCheck.NotAllowed,
            GroupRules.checkRemoveMember(creator, group, creator, 0, emptyList()),
        )
        assertEquals(
            GroupCheck.NotAllowed,
            GroupRules.checkRemoveMember(
                creator,
                group,
                guest.copy(isRemoved = true),
                0,
                emptyList(),
            ),
        )
        assertEquals(
            GroupCheck.SettleFirst(200),
            GroupRules.checkRemoveMember(creator, group, guest, -200, emptyList()),
        )
        assertEquals(
            GroupCheck.ResolvePendingFirst,
            GroupRules.checkRemoveMember(creator, group, guest, 0, listOf(pending(member, guest))),
        )
    }

    @Test
    fun `members can leave once settled, the creator archives instead`() {
        assertEquals(GroupCheck.Allowed, GroupRules.checkLeave(member, group, 0, emptyList()))
        assertEquals(
            GroupCheck.CreatorMustArchive,
            GroupRules.checkLeave(creator, group, 0, emptyList()),
        )
        assertEquals(GroupCheck.ReadOnly, GroupRules.checkLeave(member, archived, 0, emptyList()))
        assertEquals(
            GroupCheck.SettleFirst(300),
            GroupRules.checkLeave(member, group, -300, emptyList()),
        )
        assertEquals(
            GroupCheck.SettleFirst(300),
            GroupRules.checkLeave(member, group, 300, emptyList()),
        )
        assertEquals(
            GroupCheck.ResolvePendingFirst,
            GroupRules.checkLeave(member, group, 0, listOf(pending(creator, member))),
        )
        assertEquals(GroupCheck.NotAllowed, GroupRules.checkLeave(guest, group, 0, emptyList()))
    }

    @Test
    fun `pending payments not involving the member don't block them`() {
        val others = listOf(pending(creator, guest))
        assertEquals(GroupCheck.Allowed, GroupRules.checkLeave(member, group, 0, others))
        val cancelled = pending(member, guest).copy(isDeleted = true)
        assertEquals(GroupCheck.Allowed, GroupRules.checkLeave(member, group, 0, listOf(cancelled)))
    }
}
