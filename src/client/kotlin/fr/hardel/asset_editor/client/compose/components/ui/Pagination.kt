package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

private const val VISIBLE = 5

@Composable
fun Pagination(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    val count = minOf(totalPages, VISIBLE)
    val half = count / 2
    val start = maxOf(0, minOf(currentPage - half, totalPages - count))

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        NavButton("<", enabled = currentPage > 0) { onPageChange(currentPage - 1) }

        for (i in 0 until count) {
            val page = start + i
            PageButton(page, isCurrent = page == currentPage) { onPageChange(page) }
        }

        NavButton(">", enabled = currentPage < totalPages - 1) { onPageChange(currentPage + 1) }
    }
}

@Composable
private fun PageButton(page: Int, isCurrent: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(if (isCurrent) Modifier.background(VoxelColors.Zinc800) else Modifier)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Text(
            text = (page + 1).toString(),
            style = VoxelTypography.medium(12),
            color = if (isCurrent) VoxelColors.Zinc100 else VoxelColors.Zinc500
        )
    }
}

@Composable
private fun NavButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .then(
                if (enabled) Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                else Modifier
            )
    ) {
        Text(
            text = text,
            style = VoxelTypography.medium(12),
            color = if (enabled) VoxelColors.Zinc400 else VoxelColors.Zinc700
        )
    }
}
