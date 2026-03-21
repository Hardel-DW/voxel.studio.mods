package fr.hardel.asset_editor.client.compose.lib.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.text.iterator

object ColorUtils {

    fun accentColor(key: String?): Color {
        if (key.isNullOrBlank()) return hueToColor(0)
        return hueToColor(stringToHue(key.trim()))
    }

    fun stringToHue(text: String): Int {
        var hash = 0
        for (c in text) {
            hash = c.code + ((hash shl 5) - hash)
        }
        return (abs(hash.toLong()) % 360L).toInt()
    }

    fun hueToColor(hue: Int, saturation: Float = 0.70f, lightness: Float = 0.60f): Color {
        val h = ((hue % 360) + 360) % 360
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f)
        )
    }
}