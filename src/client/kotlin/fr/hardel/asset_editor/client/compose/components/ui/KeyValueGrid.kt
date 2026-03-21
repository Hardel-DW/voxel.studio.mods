package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.debug.RecordIntrospector
import fr.hardel.asset_editor.client.debug.RecordIntrospector.Field
import fr.hardel.asset_editor.client.debug.RecordIntrospector.FieldValue
import net.minecraft.client.resources.language.I18n

private const val DEFAULT_ITEMS_VISIBLE = 5

@Composable
fun KeyValueGrid(
    fields: List<Field>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (field in fields) {
            FieldRow(field, depth = 0)
        }
    }
}

@Composable
fun KeyValueGrid(
    record: Any,
    modifier: Modifier = Modifier
) {
    KeyValueGrid(RecordIntrospector.introspect(record), modifier)
}

@Composable
private fun FieldRow(field: Field, depth: Int) {
    when (val value = field.value()) {
        is FieldValue.Scalar -> ScalarRow(field.name(), value.text(), depth)
        is FieldValue.Nested -> NestedSection(field.name(), value.children(), depth)
        is FieldValue.Items -> ItemsSection(field.name(), value, depth)
    }
}

@Composable
private fun ScalarRow(key: String, value: String, depth: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .padding(start = (depth * 12).dp)
    ) {
        Text(
            text = key,
            style = VoxelTypography.medium(11),
            color = VoxelColors.Zinc500
        )
        Text(
            text = value,
            style = VoxelTypography.regular(12),
            color = VoxelColors.Zinc300,
            modifier = Modifier.weight(1f)
        )
        CopyButton(textProvider = { value })
    }
}

@Composable
private fun NestedSection(key: String, children: List<Field>, depth: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = key,
            style = VoxelTypography.semiBold(11),
            color = VoxelColors.Zinc400,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp).padding(start = (depth * 12).dp)
        )
        for (child in children) {
            FieldRow(child, depth + 1)
        }
    }
}

@Composable
private fun ItemsSection(key: String, items: FieldValue.Items, depth: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$key (${items.totalSize()})",
            style = VoxelTypography.semiBold(11),
            color = VoxelColors.Zinc400,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp).padding(start = (depth * 12).dp)
        )

        var shown = 0
        for (item in items.preview()) {
            when (item) {
                is FieldValue.Scalar -> {
                    Text(
                        text = item.text(),
                        style = VoxelTypography.regular(11),
                        color = VoxelColors.Zinc300,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp).padding(start = ((depth + 1) * 12).dp)
                    )
                    shown++
                }
                is FieldValue.Nested -> {
                    for (child in item.children()) {
                        FieldRow(child, depth + 1)
                        shown++
                    }
                }
                is FieldValue.Items -> {
                    ItemsSection("[$shown]", item, depth + 1)
                    shown++
                }
            }
        }

        if (items.totalSize() > DEFAULT_ITEMS_VISIBLE && shown < items.totalSize()) {
            val remaining = items.totalSize() - shown
            Text(
                text = I18n.get("debug:keyvalue.more", remaining),
                style = VoxelTypography.regular(11),
                color = VoxelColors.Zinc600,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp).padding(start = ((depth + 1) * 12).dp)
            )
        }
    }
}
