package com.split.easy.domain.model

/** The parts of a group that decide who may do what. */
data class Group(
    val id: String,
    /** Firebase Auth uid of the person who created the group. */
    val createdByUserId: String,
    val isArchived: Boolean = false,
)
