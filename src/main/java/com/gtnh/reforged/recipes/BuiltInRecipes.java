package com.gtnh.reforged.recipes;

import java.util.List;
import java.util.Objects;

import com.gtnh.reforged.materials.MaterialCatalog;
import com.gtnh.reforged.materials.MaterialDefinition;
import com.gtnh.reforged.materials.MaterialForm;
import com.gtnh.reforged.materials.MaterialId;

/** Initial data-only processing recipe used to exercise material-to-recipe lookup. */
public final class BuiltInRecipes {
    private BuiltInRecipes() {
    }

    public static RecipeCatalog createInitialCatalog(MaterialCatalog materials) {
        Objects.requireNonNull(materials, "materials");
        MaterialId ironId = new MaterialId("gtnh_reforged", "iron");
        MaterialDefinition iron = materials.find(ironId)
                .orElseThrow(() -> new IllegalStateException("Missing built-in material: " + ironId));
        if (!iron.forms().containsAll(List.of(MaterialForm.DUST, MaterialForm.INGOT))) {
            throw new IllegalStateException("Built-in iron material must define dust and ingot forms");
        }

        RecipeDefinition ironDustToIngot = new RecipeDefinition(
                new RecipeId("gtnh_reforged", "smelting/iron_dust"),
                "smelting",
                List.of(new MaterialAmount(ironId, MaterialForm.DUST, 1)),
                List.of(new MaterialAmount(ironId, MaterialForm.INGOT, 1)),
                "basic_electric_furnace",
                200,
                8
        );
        return new RecipeCatalog(List.of(ironDustToIngot));
    }
}