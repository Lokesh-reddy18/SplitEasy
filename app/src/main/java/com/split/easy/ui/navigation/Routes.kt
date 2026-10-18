package com.split.easy.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose destinations. */

@Serializable
data object GroupsHomeRoute

@Serializable
data object ProfileRoute

@Serializable
data object CreateGroupRoute

@Serializable
data object ArchivedGroupsRoute

@Serializable
data class GroupDetailRoute(val groupId: String)

/** Add an expense, or edit one when [expenseId] is set. */
@Serializable
data class ExpenseEditorRoute(val groupId: String, val expenseId: String? = null)

@Serializable
data class BalancesRoute(val groupId: String)

@Serializable
data class SettleUpRoute(val groupId: String, val toMemberId: String)

@Serializable
data class GroupSettingsRoute(val groupId: String)

@Serializable
data class ExpenseDetailRoute(val expenseId: String)
