package fr.hardel.asset_editor.client.compose

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VoxelTypography {
    val RubikFamily = FontFamily.Default

    fun light(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Light,
        fontSize = size.sp
    )

    fun regular(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp
    )

    fun medium(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp
    )

    fun semiBold(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp
    )

    fun bold(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp
    )

    fun extraBold(size: Int) = TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp
    )
}
