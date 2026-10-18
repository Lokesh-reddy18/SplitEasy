package com.split.easy.ui.groupsettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.split.easy.domain.group.GroupCheck
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class GroupSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val group = Group("g1", "Coorg trip", "uid-you")
    private val me = Member("you", "Ananya", "uid-you", upiId = "ananya.r@okaxis")

    private val state = GroupSettingsUiState(
        isLoading = false,
        group = group,
        me = me,
        isCreator = true,
        memberActionCheck = GroupCheck.Allowed,
        archiveCheck = GroupCheck.SettleFirst(3_50_000),
        members = listOf(
            MemberRow(me, isMe = true, isCreator = true, removeCheck = GroupCheck.NotAllowed),
            MemberRow(
                Member("priya", "Priya", "uid-p", upiId = "priya.s@okicici"),
                false,
                false,
                GroupCheck.SettleFirst(30_000),
            ),
            MemberRow(
                Member("arjun", "Arjun", null, upiId = "arjun@oksbi"),
                false,
                false,
                GroupCheck.ResolvePendingFirst,
            ),
            MemberRow(Member("nikhil", "Nikhil", null), false, false, GroupCheck.Allowed),
        ),
    )

    @Test
    fun `shows members, reasons and the creator note like design 12`() {
        val removed = mutableListOf<String>()
        composeRule.setContent {
            SplitEasyTheme {
                GroupSettingsScreen(
                    state,
                    SettingsActions(onRemove = {
                        removed +=
                            it
                    }),
                )
            }
        }

        composeRule.onNodeWithText("MEMBERS · 4").assertIsDisplayed()
        composeRule.onNodeWithText("Group creator · ananya.r@okaxis").assertIsDisplayed()
        composeRule.onNodeWithText("Settle ₹300 first").assertIsDisplayed()
        composeRule.onNodeWithText("Resolve pending payments first").assertIsDisplayed()
        assertEquals(2, composeRule.onAllNodesWithText("Guest").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Settle ₹3,500 first").assertIsDisplayed()
        composeRule.onNodeWithText(
            "As the creator you can't leave the group. Archive it once everyone has settled up.",
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNodeWithText("Remove Nikhil?").assertIsDisplayed()
        composeRule.onAllNodesWithText("Remove")[1].performClick()
        assertEquals(listOf("nikhil"), removed)
    }

    @Test
    fun `a member who isn't the creator sees Leave group`() {
        val member = state.copy(
            isCreator = false,
            leaveCheck = GroupCheck.Allowed,
            members = state.members.map {
                it.copy(isCreator = false)
            },
        )
        var left = 0
        composeRule.setContent {
            SplitEasyTheme { GroupSettingsScreen(member, SettingsActions(onLeave = { left++ })) }
        }
        composeRule.onNodeWithText("Remove").assertDoesNotExist()
        composeRule.onNodeWithText("Leave group").performClick()
        composeRule.onNodeWithText("Leave Coorg trip?").assertIsDisplayed()
        composeRule.onAllNodesWithText("Leave group")[1].performClick()
        assertEquals(1, left)
    }
}
