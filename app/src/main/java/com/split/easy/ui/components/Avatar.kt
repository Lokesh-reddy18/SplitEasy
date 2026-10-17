package com.split.easy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.split.easy.ui.theme.SplitEasyColors
import com.split.easy.ui.theme.SplitEasyTheme

/** The four tile colours used for avatars in the designs. */
enum class AvatarTint {
    PRIMARY,
    SECONDARY,
    OWE,
    NEUTRAL,
}

/** Picks a tint from a stable key (such as a member id), so a person keeps the same colour. */
fun avatarTintFor(key: String): AvatarTint {
    val tints = AvatarTint.entries
    return tints[(key.hashCode() and Int.MAX_VALUE) % tints.size]
}

internal fun SplitEasyColors.tintColors(tint: AvatarTint): Pair<Color, Color> = when (tint) {
    AvatarTint.PRIMARY -> primaryTint to onPrimaryTint
    AvatarTint.SECONDARY -> secondaryTint to onSecondaryTint
    AvatarTint.OWE -> oweTint to onOweTint
    AvatarTint.NEUTRAL -> neutralTint to onNeutralTint
}

/** People are circles; groups are rounded squares (see 02 and 05). It's decorative for TalkBack. */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    tint: AvatarTint = avatarTintFor(name),
) {
    val (background, content) = SplitEasyTheme.colors.tintColors(tint)
    Box(
        modifier = modifier
            .size(size)
            .background(background, shape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = content,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}

val GroupAvatarShape = RoundedCornerShape(12.dp)
