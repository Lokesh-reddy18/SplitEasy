package com.split.easy.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.split.easy.R
import com.split.easy.domain.model.Category
import com.split.easy.ui.theme.SplitEasyTheme

/** One icon and tile colour per category (Q12). */
data class CategoryStyle(
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
    val tint: AvatarTint,
)

fun Category.style(): CategoryStyle = when (this) {
    Category.FOOD -> CategoryStyle(
        R.drawable.ic_category_food,
        R.string.category_food,
        AvatarTint.OWE,
    )

    Category.GROCERIES ->
        CategoryStyle(R.drawable.ic_category_groceries, R.string.category_groceries, AvatarTint.OWE)

    Category.TRAVEL -> CategoryStyle(
        R.drawable.ic_category_travel,
        R.string.category_travel,
        AvatarTint.SECONDARY,
    )

    Category.FUEL -> CategoryStyle(
        R.drawable.ic_category_fuel,
        R.string.category_fuel,
        AvatarTint.SECONDARY,
    )

    Category.STAY -> CategoryStyle(
        R.drawable.ic_category_stay,
        R.string.category_stay,
        AvatarTint.PRIMARY,
    )

    Category.RENT -> CategoryStyle(
        R.drawable.ic_category_rent,
        R.string.category_rent,
        AvatarTint.PRIMARY,
    )

    Category.UTILITIES ->
        CategoryStyle(
            R.drawable.ic_category_utilities,
            R.string.category_utilities,
            AvatarTint.SECONDARY,
        )

    Category.SHOPPING ->
        CategoryStyle(R.drawable.ic_category_shopping, R.string.category_shopping, AvatarTint.OWE)

    Category.ENTERTAINMENT ->
        CategoryStyle(
            R.drawable.ic_category_entertainment,
            R.string.category_entertainment,
            AvatarTint.PRIMARY,
        )

    Category.OTHER -> CategoryStyle(
        R.drawable.ic_category_other,
        R.string.category_other,
        AvatarTint.NEUTRAL,
    )
}

/** The rounded tile at the start of an expense row. Decorative: the row's text names the expense. */
@Composable
fun CategoryIcon(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val style = category.style()
    val (background, content) = SplitEasyTheme.colors.tintColors(style.tint)
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(style.icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size / 2),
        )
    }
}
