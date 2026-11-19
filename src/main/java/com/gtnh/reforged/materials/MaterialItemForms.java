package com.gtnh.reforged.materials;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import com.gtnh.reforged.registry.ModItems;

/**
 * Bridges logical material forms to registered game items.
 *
 * <p>This is intentionally a small bootstrap mapping, not a generated item system yet.
 */
public final class MaterialItemForms {
    private static final MaterialId IRON = new MaterialId("gtnh_reforged", "iron");
    private static final Map<MaterialForm, DeferredItem<Item>> IRON_FORMS;

    static {
        EnumMap<MaterialForm, DeferredItem<Item>> forms = new EnumMap<>(MaterialForm.class);
        forms.put(MaterialForm.DUST, ModItems.IRON_DUST);
        forms.put(MaterialForm.INGOT, ModItems.IRON_INGOT);
        IRON_FORMS = Map.copyOf(forms);
    }

    private MaterialItemForms() {
    }

    public static Optional<DeferredItem<Item>> find(MaterialId material, MaterialForm form) {
        if (!IRON.equals(material)) {
            return Optional.empty();
        }
        return Optional.ofNullable(IRON_FORMS.get(form));
    }
}
