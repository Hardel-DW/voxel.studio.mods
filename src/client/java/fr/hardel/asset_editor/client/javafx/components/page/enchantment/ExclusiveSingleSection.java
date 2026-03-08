package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import javafx.scene.layout.VBox;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class ExclusiveSingleSection extends VBox {

    public ExclusiveSingleSection(StudioContext context, Identifier currentElementId) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        List<Identifier> custom = context.allTypedEntries(Registries.ENCHANTMENT).stream()
            .map(ElementEntry::id)
            .filter(id -> !id.getNamespace().equals("minecraft"))
            .toList();

        List<Identifier> vanilla = context.allTypedEntries(Registries.ENCHANTMENT).stream()
            .map(ElementEntry::id)
            .filter(id -> id.getNamespace().equals("minecraft"))
            .toList();

        getChildren().add(new EnchantmentCategory("enchantment:exclusive.custom.title", custom, context, currentElementId));
        if (!vanilla.isEmpty()) {
            getChildren().add(new EnchantmentCategory("enchantment:exclusive.vanilla.title", vanilla, context, currentElementId));
        }
    }
}
