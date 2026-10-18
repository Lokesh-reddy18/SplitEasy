package com.split.easy.ui.expense.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.split.easy.domain.money.groupIndianDigits

/**
 * Shows "240000.5" as "2,40,000.5" while typing, as in the design. The field's real value keeps
 * no commas, so parsing stays simple; the offset mapping keeps the cursor in the right place.
 */
class IndianGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dot = raw.indexOf('.')
        val wholePart = if (dot >= 0) raw.substring(0, dot) else raw
        val shown = groupIndianDigits(wholePart) + raw.substring(wholePart.length)

        // originalToShown[i] is where the i-th typed character appears; the last entry is the end.
        val originalToShown = IntArray(raw.length + 1)
        var original = 0
        shown.forEachIndexed { index, char ->
            if (char != ',') originalToShown[original++] = index
        }
        originalToShown[raw.length] = shown.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                originalToShown[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int) =
                shown.take(offset.coerceIn(0, shown.length)).count { it != ',' }
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
