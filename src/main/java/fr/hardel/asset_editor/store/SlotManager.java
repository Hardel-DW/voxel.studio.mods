package fr.hardel.asset_editor.store;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SlotManager {

    private static final List<EquipmentSlotGroup> COMPOSITES = List.of(
        EquipmentSlotGroup.ANY, EquipmentSlotGroup.ARMOR, EquipmentSlotGroup.HAND);

    private final Set<EquipmentSlot> active;

    public SlotManager(List<EquipmentSlotGroup> groups) {
        active = new LinkedHashSet<>();
        for (var group : groups)
            active.addAll(group.slots());
    }

    public SlotManager toggle(EquipmentSlotGroup group) {
        var slots = group.slots();
        if (active.containsAll(slots))
            slots.forEach(active::remove);
        else
            active.addAll(slots);
        return this;
    }

    public List<EquipmentSlotGroup> toGroups() {
        var remaining = new LinkedHashSet<>(active);
        List<EquipmentSlotGroup> result = new ArrayList<>();

        for (var composite : COMPOSITES) {
            if (remaining.containsAll(composite.slots())) {
                result.add(composite);
                composite.slots().forEach(remaining::remove);
            }
        }

        for (var slot : remaining)
            result.add(EquipmentSlotGroup.bySlot(slot));
        return List.copyOf(result);
    }
}
