package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Exclusive single mode: custom (from datapack) + vanilla enchantment lists.
 * Matches ExclusiveSingleSection.tsx.
 */
public final class ExclusiveSingleSection extends VBox {

    public ExclusiveSingleSection(StudioContext context) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        List<Identifier> custom = context.repository().enchantments().stream()
            .map(e -> Identifier.fromNamespaceAndPath(e.namespace(), e.resource()))
            .filter(id -> !id.getNamespace().equals("minecraft"))
            .toList();

        List<Identifier> vanilla = context.repository().enchantments().stream()
            .map(e -> Identifier.fromNamespaceAndPath(e.namespace(), e.resource()))
            .filter(id -> id.getNamespace().equals("minecraft"))
            .toList();

        getChildren().add(new EnchantmentCategory("enchantment:exclusive.custom.title", custom));
        if (!vanilla.isEmpty()) {
            getChildren().add(new EnchantmentCategory("enchantment:exclusive.vanilla.title", vanilla));
        }
    }
}
