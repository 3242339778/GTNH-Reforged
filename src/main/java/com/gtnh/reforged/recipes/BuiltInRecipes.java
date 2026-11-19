package com.gtnh.reforged.recipes;

import java.util.List;

import com.gtnh.reforged.materials.MaterialForm;
import com.gtnh.reforged.materials.MaterialId;

/** Initial data-only processing recipe used to exercise material-to-recipe lookup. */
public final class BuiltInRecipes {
    private BuiltInRecipes() {
    }

    public static RecipeCatalog createInitialCatalog() {
        MaterialId iron = new MaterialId("gtnh_reforged", "iron");
        RecipeDefinition ironDustToIngot = new RecipeDefinition(
                new RecipeId("gtnh_reforged", "smelting/iron_dust"),
                "smelting",
                List.of(new MaterialAmount(iron, MaterialForm.DUST, 1)),
                List.of(new MaterialAmount(iron, MaterialForm.INGOT, 1)),
                "basic_electric_furnace",
                200,
                8
        );
        return new RecipeCatalog(List.of(ironDustToIngot));
    }
}