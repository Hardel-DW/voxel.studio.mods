package fr.hardel.asset_editor.client.compose

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object StudioColors {
    // Tailwind Colors
    val Zinc50 = Color(0xFFFAFAFA)
    val Zinc100 = Color(0xFFF4F4F5)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc600 = Color(0xFF52525B)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc800 = Color(0xFF27272A)
    val Zinc900 = Color(0xFF18181B)
    val Zinc925 = Color(0xFF101011)
    val Zinc950 = Color(0xFF09090B)
    val Red300 = Color(0xFFFCA5A5)
    val Red400 = Color(0xFFF87171)
    val Red500 = Color(0xFFEF4444)
    val Sky400 = Color(0xFF38BDF8)
    val Amber400 = Color(0xFFFBBF24)
    val Emerald400 = Color(0xFF34D399)
    val Green500 = Color(0xFF22C55E)
    val Blue500 = Color(0xFF3B82F6)
    val Violet500 = Color(0xFF8B5CF6)
    val Orange700 = Color(0xFFC2410C)

    // Syntax highlighting (One Dark palette)
    val SyntaxString = Color(0xFF98C379)
    val SyntaxNumber = Color(0xFFD19A66)
    val SyntaxBoolean = Color(0xFF56B6C2)
    val SyntaxNull = Color(0xFFC678DD)
    val SyntaxProperty = Color(0xFF61AFEF)
    val SyntaxPunctuation = Color(0xFFABB2BF)

    // Enchanting table palette
    val EnchantSelected = Color(0xFF7A009F)
    val EnchantHover = Color(0xFF52006F)
    val EnchantSelectedBg = Color(0xFF1A0026)
    val EnchantHoverBg = Color(0xFF120018)

    // Switch
    val CheckedRail = Brush.linearGradient(listOf(Color(0xFF180909), Color(0xFF7A009F)))
    val CheckedCircle = Color(0xFFFFFFFF)
    val UncheckedRail = Color(0xFF343434)
    val UncheckedCircle = Color(0xFFDFDFDF)

    // Minecraft in-game tooltip / experience palette
    val Experience = Color(0xFF80FF20)
    val TooltipName = Color(0xFFFAFAFA)
    val TooltipEnchant = Color(0xFFA8A8A8)
}
