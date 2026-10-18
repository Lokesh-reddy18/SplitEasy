package com.split.easy.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.Group
import com.split.easy.domain.usecase.GroupSummary
import com.split.easy.domain.usecase.HomeSummary
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screen size of the design frames (390 x 844 dp). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class GroupsHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val user = CurrentUser("local-user", "You", isPlaceholder = true)

    /** The groups from the design (02). */
    private val designGroups = listOf(
        GroupSummary(
            Group("flat", "Flat 4B", "u"),
            memberCount = 4,
            myNetPaise = 1_85_000,
            waitingForMe = 0,
        ),
        GroupSummary(
            Group("coorg", "Coorg trip", "u"),
            memberCount = 5,
            myNetPaise = 1_20_000,
            waitingForMe = 1,
        ),
        GroupSummary(
            Group("lunch", "Office lunches", "u"),
            memberCount = 6,
            myNetPaise = -60_000,
            waitingForMe = 0,
        ),
        GroupSummary(Group("home", "Home", "u"), memberCount = 2, myNetPaise = 0, waitingForMe = 0),
    )

    private var opened: String? = null
    private var addExpenseClicks = 0

    private fun show(state: HomeUiState) {
        composeRule.setContent {
            SplitEasyTheme {
                GroupsHomeScreen(
                    state = state,
                    onOpenGroup = { opened = it },
                    onNewGroup = {},
                    onAddExpense = { addExpenseClicks++ },
                    onOpenArchived = {},
                    onOpenProfile = {},
                )
            }
        }
    }

    @Test
    fun `design example shows totals, balances, badge and settled up`() {
        show(
            HomeUiState(
                user,
                isLoading = false,
                summary = HomeSummary(designGroups),
                archivedCount = 2,
            ),
        )

        composeRule.onNodeWithText("Overall, you are owed").assertIsDisplayed()
        composeRule.onNodeWithText("₹2,450").assertIsDisplayed()
        // "You owe ₹600" on the summary card and on the Office lunches row.
        assertEquals(2, composeRule.onAllNodesWithText("₹600").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("₹3,050").assertIsDisplayed()
        composeRule.onNodeWithText("1 to confirm").assertIsDisplayed()
        composeRule.onNodeWithText("settled up").assertIsDisplayed()
        composeRule.onNodeWithText("6 members").assertIsDisplayed()
        composeRule.onNodeWithText("Archived groups (2)").assertExists()
        composeRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun `tapping a group opens it and Add expense asks for a group`() {
        show(HomeUiState(user, isLoading = false, summary = HomeSummary(designGroups)))

        composeRule.onNodeWithText("Coorg trip").performClick()
        assertEquals("coorg", opened)
        composeRule.onNodeWithText("Add expense", useUnmergedTree = true).performClick()
        assertEquals(1, addExpenseClicks)
    }

    @Test
    fun `no groups shows the empty card and no Add expense button`() {
        show(HomeUiState(user, isLoading = false))

        composeRule.onNodeWithText("You're all settled up").assertIsDisplayed()
        composeRule.onNodeWithText("No groups yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add expense", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `owing overall changes the summary label`() {
        val owing = listOf(designGroups[2])
        show(HomeUiState(user, isLoading = false, summary = HomeSummary(owing)))
        composeRule.onNodeWithText("Overall, you owe").assertIsDisplayed()
    }

    @Test
    fun `picker lists the last-used group first`() {
        val state =
            HomeUiState(user, summary = HomeSummary(designGroups), lastUsedGroupId = "lunch")
        assertEquals(
            listOf("lunch", "flat", "coorg", "home"),
            state.pickerGroups.map {
                it.group.id
            },
        )

        var picked: String? = null
        composeRule.setContent {
            SplitEasyTheme {
                GroupPickerContent(state.pickerGroups, state.lastUsedGroupId, onPick = {
                    picked = it
                }, onNewGroup = {})
            }
        }
        composeRule.onNodeWithText("Last used").assertIsDisplayed()
        composeRule.onNodeWithText("Home").performClick()
        assertEquals("home", picked)
    }
}
