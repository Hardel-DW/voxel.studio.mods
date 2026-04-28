package fr.hardel.asset_editor.client.compose.components.mcdoc

import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong

internal fun OptionalDouble.orNull(): Double? = if (isPresent) asDouble else null
internal fun OptionalInt.orNull(): Int? = if (isPresent) asInt else null
internal fun OptionalLong.orNull(): Long? = if (isPresent) asLong else null
