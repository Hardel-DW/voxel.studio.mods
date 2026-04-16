package fr.hardel.asset_editor.client.compose.components.page.debug

import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DEBUG_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
