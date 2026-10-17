package com.split.easy.domain.repository

import com.split.easy.domain.model.Group
import kotlinx.coroutines.flow.Flow

/**
 * Groups. Like every repository, writes go to the local database first, are marked as waiting to
 * sync, and return straight away; screens update from the Flows (FR-6).
 */
interface GroupRepository {
    /** Not archived, most recent activity first. */
    fun observeActiveGroups(): Flow<List<Group>>

    fun observeArchivedGroups(): Flow<List<Group>>

    fun observeGroup(groupId: String): Flow<Group?>

    /** Creates the group with the current user as creator and the named guests. Returns its id. */
    suspend fun createGroup(name: String, guestNames: List<String>): String

    suspend fun renameGroup(groupId: String, name: String)

    suspend fun setArchived(groupId: String, isArchived: Boolean)
}
