package com.split.easy.domain.model

/** Kinds of entries in a group's append-only event log, shown in the group feed. */
enum class GroupEventType {
    /** Someone set or changed a guest's UPI ID (F1, F10). */
    GUEST_UPI_CHANGED,
}
