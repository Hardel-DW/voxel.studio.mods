package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record ToggleTagAction(Identifier tagId) implements EditorAction {

    public static final EditorActionType<Enchantment, ToggleTagAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/toggle_tag"),
        ToggleTagAction.class,
        StreamCodec.composite(Identifier.STREAM_CODEC, ToggleTagAction::tagId, ToggleTagAction::new),
        (entry, action, ctx) -> entry.toggleTag(action.tagId()));

    @Override
    public EditorActionType<Enchantment, ToggleTagAction> type() {
        return TYPE;
    }
}
