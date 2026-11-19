package com.gtnh.reforged.materials;

import java.util.EnumSet;
import java.util.List;

/** Initial in-code material sample used to exercise the domain model. */
public final class BuiltInMaterials {
    private BuiltInMaterials() {
    }

    public static MaterialCatalog createInitialCatalog() {
        MaterialDefinition iron = new MaterialDefinition(
                new MaterialId("gtnh_reforged", "iron"),
                EnumSet.of(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.PLATE, MaterialForm.ROD),
                0xD8D8D8
        );
        return new MaterialCatalog(List.of(iron));
    }
}