package com.split.easy.ui.expense

import androidx.compose.ui.text.AnnotatedString
import com.split.easy.ui.expense.editor.IndianGroupingTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class IndianGroupingTransformationTest {

    private fun transform(text: String) =
        IndianGroupingTransformation().filter(AnnotatedString(text))

    @Test
    fun `typed amounts get lakh grouping`() {
        assertEquals("2,400", transform("2400").text.text)
        assertEquals("2,40,000.5", transform("240000.5").text.text)
        assertEquals("999", transform("999").text.text)
        assertEquals("", transform("").text.text)
        assertEquals(".5", transform(".5").text.text)
    }

    @Test
    fun `cursor positions map across the commas`() {
        val mapping = transform("240000").offsetMapping // shown as 2,40,000
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1)) // after "2,"
        assertEquals(8, mapping.originalToTransformed(6)) // end
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(6, mapping.transformedToOriginal(8))
    }
}
