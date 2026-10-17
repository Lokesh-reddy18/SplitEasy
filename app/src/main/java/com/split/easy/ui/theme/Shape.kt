package com.split.easy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner sizes from the designs: buttons 12, cards 16, summary card 20, sheets and dialogs 24. */
val SplitEasyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** The floating "Add expense" button. */
val FabShape = RoundedCornerShape(18.dp)
