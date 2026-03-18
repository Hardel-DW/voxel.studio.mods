package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.network.EditorAction;
import fr.hardel.asset_editor.store.ElementEntry;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public final class ExclusiveSingleSection extends VBox {

    public ExclusiveSingleSection(StudioContext context,
        StoreSelection<ElementEntry<?>, Set<String>> directExclusiveSelection,
        FxSelectionBindings bindings,
        BiFunction<UnaryOperator<Enchantment>, EditorAction, Boolean> applyMutation) {
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

        if (!vanilla.isEmpty()) {
            getChildren().add(new EnchantmentCategory(
                I18n.get("enchantment.exclusive:vanilla"),
                vanilla,
                directExclusiveSelection,
                bindings,
                applyMutation,
                context));
        }
        getChildren().add(new EnchantmentCategory(
            I18n.get("enchantment.exclusive:custom"),
            custom,
            directExclusiveSelection,
            bindings,
            applyMutation,
            context));
    }
}
