package fr.hardel.asset_editor.client.compose

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

object StudioTypography {
    private const val FONT_ROOT = "assets/asset_editor/fonts/"

    val RubikFamily = FontFamily(
        Font("${FONT_ROOT}rubik-light.ttf", FontWeight.Light),
        Font("${FONT_ROOT}rubik-regular.ttf", FontWeight.Normal),
        Font("${FONT_ROOT}rubik-medium.ttf", FontWeight.Medium),
        Font("${FONT_ROOT}rubik-semibold.ttf", FontWeight.SemiBold),
        Font("${FONT_ROOT}rubik-bold.ttf", FontWeight.Bold),
        Font("${FONT_ROOT}rubik-extrabold.ttf", FontWeight.ExtraBold),
        Font("${FONT_ROOT}rubik-black.ttf", FontWeight.Black)
    )

    val MinecraftFamily = FontFamily(
        Font("${FONT_ROOT}minecraftten.ttf", FontWeight.Normal)
    )

    val SevenFamily = FontFamily(
        Font("${FONT_ROOT}seven.ttf", FontWeight.Normal)
    )

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

    fun minecraftTen(size: Int) = TextStyle(
        fontFamily = MinecraftFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp
    )

    fun seven(size: Int) = TextStyle(
        fontFamily = SevenFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp
    )
}
