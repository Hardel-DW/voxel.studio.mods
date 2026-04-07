package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

public record SetIntFieldAction(String field, int value) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, SetIntFieldAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetIntFieldAction::field,
        ByteBufCodecs.VAR_INT, SetIntFieldAction::value,
        SetIntFieldAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        Enchantment e = entry.data();
        EnchantmentDefinition d = e.definition();
        EnchantmentDefinition next = switch (field) {
            case "max_level" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), value, d.minCost(), d.maxCost(), d.anvilCost(), d.slots());
            case "weight" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), value, d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots());
            case "anvil_cost" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), value, d.slots());
            case "min_cost_base" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), new Cost(value, d.minCost().perLevelAboveFirst()), d.maxCost(), d.anvilCost(), d.slots());
            case "min_cost_per_level" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), new Cost(d.minCost().base(), value), d.maxCost(), d.anvilCost(), d.slots());
            case "max_cost_base" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), new Cost(value, d.maxCost().perLevelAboveFirst()), d.anvilCost(), d.slots());
            case "max_cost_per_level" -> new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), new Cost(d.maxCost().base(), value), d.anvilCost(), d.slots());
            default -> d;
        };
        return entry.withData(new Enchantment(e.description(), next, e.exclusiveSet(), e.effects()));
    }
}
