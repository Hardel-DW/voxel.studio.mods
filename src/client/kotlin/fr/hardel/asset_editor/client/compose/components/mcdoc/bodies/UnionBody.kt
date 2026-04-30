package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.heads.selectMember
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.UnionType

@Composable
fun UnionBody(
    type: UnionType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val members = type.members()
    if (members.isEmpty()) return
    val activeIndex = selectMember(members, value)
    val active = members.getOrNull(activeIndex) ?: members.first()
    val activeSimplified = rememberSimplified(active, value)
    if (!hasMcdocBody(activeSimplified, value)) return
    Body(activeSimplified, value, onValueChange, modifier)
}
