package com.split.easy.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun `two or more words use the first and last words`() {
        assertEquals("CT", initialsOf("Coorg trip"))
        assertEquals("OL", initialsOf("Office lunches"))
        assertEquals("PS", initialsOf("Priya Sharma"))
        assertEquals("AR", initialsOf("Ananya  Rao"))
        assertEquals("MB", initialsOf("Meera's big birthday"))
    }

    @Test
    fun `digits and symbols are skipped`() {
        assertEquals("FB", initialsOf("Flat 4B"))
        assertEquals("GT", initialsOf("Goa 2026 trip!"))
    }

    @Test
    fun `one word uses its first letter and next consonant`() {
        assertEquals("HM", initialsOf("Home"))
        assertEquals("PR", initialsOf("Priya"))
        // No consonant after the first letter, so the second letter is used.
        assertEquals("AA", initialsOf("Aai"))
    }

    @Test
    fun `short and empty names`() {
        assertEquals("A", initialsOf("a"))
        assertEquals("?", initialsOf("   "))
        assertEquals("?", initialsOf("123"))
    }
}
