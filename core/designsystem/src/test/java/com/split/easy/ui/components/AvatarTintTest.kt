package com.split.easy.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarTintTest {

    @Test
    fun `the same key always gets the same tint`() {
        repeat(3) { assertEquals(avatarTintFor("member-123"), avatarTintFor("member-123")) }
    }

    @Test
    fun `different keys use all four tints`() {
        val tints = (1..200).map { avatarTintFor("member-$it") }.toSet()
        assertEquals(AvatarTint.entries.toSet(), tints)
    }

    @Test
    fun `keys with negative hash codes are handled`() {
        val key = "polygenelubricants" // String.hashCode() is Int.MIN_VALUE
        assertTrue(key.hashCode() == Int.MIN_VALUE)
        avatarTintFor(key)
    }
}
