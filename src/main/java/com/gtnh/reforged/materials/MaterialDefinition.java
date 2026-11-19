package com.gtnh.reforged.materials;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable domain metadata for one material, independent of Minecraft item classes. */
public record MaterialDefinition(MaterialId id, Set<MaterialForm> forms, int colorRgb) {
    public MaterialDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(forms, "forms");
        if (forms.isEmpty()) {
            throw new IllegalArgumentException("A material must define at least one form: " + id);
        }
        if ((colorRgb & 0xFF000000) != 0) {
            throw new IllegalArgumentException("Material color must be a 24-bit RGB value: " + id);
        }
        forms = Collections.unmodifiableSet(EnumSet.copyOf(forms));
    }
}