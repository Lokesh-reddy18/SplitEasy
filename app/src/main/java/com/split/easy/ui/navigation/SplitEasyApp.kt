package com.split.easy.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.split.easy.R
import com.split.easy.ui.archived.ArchivedGroupsRoute
import com.split.easy.ui.balances.BalancesRoute
import com.split.easy.ui.common.ComingSoonScreen
import com.split.easy.ui.creategroup.CreateGroupRoute
import com.split.easy.ui.expense.detail.ExpenseDetailRoute
import com.split.easy.ui.expense.editor.ExpenseEditorRoute
import com.split.easy.ui.groupdetail.GroupDetailRoute
import com.split.easy.ui.home.GroupsHomeRoute

/** The app's screens. The bottom bar shows only on the top-level tabs. */
@Composable
fun SplitEasyApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedTab = when {
        destination?.hasRoute<GroupsHomeRoute>() == true -> TopLevelTab.GROUPS
        destination?.hasRoute<ProfileRoute>() == true -> TopLevelTab.PROFILE
        else -> null
    }

    Scaffold(
        // Each screen handles its own status-bar inset; the Scaffold only reserves the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (selectedTab != null) {
                SplitEasyBottomBar(selected = selectedTab, onSelect = { navController.openTab(it) })
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = GroupsHomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<GroupsHomeRoute> {
                GroupsHomeRoute(
                    onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
                    onNewGroup = { navController.navigate(CreateGroupRoute) },
                    onAddExpense = { navController.navigate(ExpenseEditorRoute(it)) },
                    onOpenArchived = { navController.navigate(ArchivedGroupsRoute) },
                    onOpenProfile = { navController.openTab(TopLevelTab.PROFILE) },
                )
            }
            composable<CreateGroupRoute> {
                CreateGroupRoute(
                    onClose = { navController.popBackStack() },
                    onCreated = { groupId ->
                        // Replace the form with the new group, so Back goes home.
                        navController.navigate(GroupDetailRoute(groupId)) {
                            popUpTo<CreateGroupRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<ArchivedGroupsRoute> {
                ArchivedGroupsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
                )
            }
            // Built in later steps of M2; placeholders keep navigation working until then.
            composable<GroupDetailRoute> {
                GroupDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(GroupSettingsRoute(it)) },
                    onOpenBalances = { navController.navigate(BalancesRoute(it)) },
                    onSettleUp = { groupId, toMemberId ->
                        navController.navigate(SettleUpRoute(groupId, toMemberId))
                    },
                    onAddExpense = { navController.navigate(ExpenseEditorRoute(it)) },
                    onOpenExpense = { navController.navigate(ExpenseDetailRoute(it)) },
                )
            }
            composable<BalancesRoute> {
                BalancesRoute(
                    onBack = { navController.popBackStack() },
                    onPay = { groupId, toMemberId ->
                        navController.navigate(SettleUpRoute(groupId, toMemberId))
                    },
                )
            }
            composable<SettleUpRoute> { Placeholder(R.string.group_settle_up, navController) }
            composable<GroupSettingsRoute> { Placeholder(R.string.group_settings, navController) }
            composable<ExpenseDetailRoute> {
                ExpenseDetailRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { groupId, expenseId ->
                        navController.navigate(ExpenseEditorRoute(groupId, expenseId))
                    },
                )
            }
            composable<ExpenseEditorRoute> {
                ExpenseEditorRoute(onClose = { navController.popBackStack() })
            }
            composable<ProfileRoute> {
                ComingSoonScreen(stringResource(R.string.nav_profile), onBack = null)
            }
        }
    }
}

/** Standard bottom-tab behaviour: one copy of each tab, state kept when switching. */
private fun NavHostController.openTab(tab: TopLevelTab) {
    val route: Any = when (tab) {
        TopLevelTab.GROUPS -> GroupsHomeRoute
        TopLevelTab.PROFILE -> ProfileRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Screens still to be built in later M2 steps. */
@Composable
private fun Placeholder(@StringRes title: Int, navController: NavHostController) {
    ComingSoonScreen(stringResource(title), onBack = { navController.popBackStack() })
}
