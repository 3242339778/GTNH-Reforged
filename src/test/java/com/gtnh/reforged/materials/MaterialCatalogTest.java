package com.gtnh.reforged.materials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class MaterialCatalogTest {
    @Test
    void initialCatalogContainsIronAndItsForms() {
        MaterialCatalog catalog = BuiltInMaterials.createInitialCatalog();
        MaterialId ironId = new MaterialId("gtnh_reforged", "iron");

        assertEquals(1, catalog.size());
        assertTrue(catalog.find(ironId).isPresent());
        assertTrue(catalog.find(ironId).orElseThrow().forms().contains(MaterialForm.INGOT));
        assertEquals(1, catalog.withForm(MaterialForm.DUST).size());
        assertTrue(catalog.withForm(MaterialForm.GEAR).isEmpty());
    }

    @Test
    void duplicateIdsAreRejected() {
        MaterialDefinition first = material("copper");
        MaterialDefinition second = material("copper");

        assertThrows(IllegalArgumentException.class, () -> new MaterialCatalog(List.of(first, second)));
    }

    @Test
    void definitionsDefensivelyCopyForms() {
        EnumSet<MaterialForm> forms = EnumSet.of(MaterialForm.INGOT);
        MaterialDefinition definition = new MaterialDefinition(new MaterialId("gtnh_reforged", "tin"), forms, 0xAAAAAA);
        forms.add(MaterialForm.PLATE);

        assertFalse(definition.forms().contains(MaterialForm.PLATE));
        assertThrows(UnsupportedOperationException.class, () -> definition.forms().add(MaterialForm.DUST));
    }

    @Test
    void invalidRgbValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaterialDefinition(new MaterialId("gtnh_reforged", "lead"), EnumSet.of(MaterialForm.INGOT), 0x1_000000));
    }

    private static MaterialDefinition material(String path) {
        return new MaterialDefinition(new MaterialId("gtnh_reforged", path), EnumSet.of(MaterialForm.INGOT), 0x808080);
    }
}