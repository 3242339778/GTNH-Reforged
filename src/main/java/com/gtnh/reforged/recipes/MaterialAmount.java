package com.gtnh.reforged.recipes;

import java.util.Objects;

import com.gtnh.reforged.materials.MaterialForm;
import com.gtnh.reforged.materials.MaterialId;

/** A quantity of one logical material form used by a recipe. */
public record MaterialAmount(MaterialId material, MaterialForm form, int amount) {
    public MaterialAmount {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(form, "form");
        if (amount <= 0) {
            throw new IllegalArgumentException("Material amount must be positive");
        }
    }
}