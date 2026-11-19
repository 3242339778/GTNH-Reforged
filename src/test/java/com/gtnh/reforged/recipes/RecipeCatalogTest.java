package com.gtnh.reforged.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gtnh.reforged.materials.BuiltInMaterials;
import com.gtnh.reforged.materials.MaterialCatalog;
import com.gtnh.reforged.materials.MaterialForm;
import com.gtnh.reforged.materials.MaterialId;

class RecipeCatalogTest {
    private static final MaterialId IRON = new MaterialId("gtnh_reforged", "iron");

    @Test
    void initialRecipeCanBeFoundByInputOutputAndMachine() {
        RecipeCatalog catalog = initialRecipes();

        assertEquals(1, catalog.accepting(IRON, MaterialForm.DUST).size());
        assertEquals(1, catalog.producing(IRON, MaterialForm.INGOT).size());
        assertEquals(1, catalog.forMachine("basic_electric_furnace").size());
        assertTrue(catalog.accepting(IRON, MaterialForm.PLATE).isEmpty());
    }

    @Test
    void duplicateRecipeIdsAreRejected() {
        RecipeDefinition recipe = initialRecipes().all().getFirst();
        assertThrows(IllegalArgumentException.class, () -> new RecipeCatalog(List.of(recipe, recipe)));
    }

    @Test
    void recipeCollectionsAreImmutable() {
        RecipeDefinition recipe = initialRecipes().all().getFirst();
        assertThrows(UnsupportedOperationException.class, () -> recipe.inputs().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                initialRecipes().accepting(IRON, MaterialForm.DUST).clear());
    }

    @Test
    void invalidRecipeAmountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MaterialAmount(IRON, MaterialForm.DUST, 0));
    }

    @Test
    void builtInRecipeRequiresItsMaterialAndForms() {
        MaterialCatalog empty = new MaterialCatalog(List.of());
        assertThrows(IllegalStateException.class, () -> BuiltInRecipes.createInitialCatalog(empty));
    }

    private static RecipeCatalog initialRecipes() {
        return BuiltInRecipes.createInitialCatalog(BuiltInMaterials.createInitialCatalog());
    }
}