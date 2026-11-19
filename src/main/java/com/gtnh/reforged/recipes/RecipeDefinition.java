package com.gtnh.reforged.recipes;

import java.util.List;
import java.util.Objects;

/** Immutable processing recipe metadata, independent of Minecraft item and machine classes. */
public record RecipeDefinition(
        RecipeId id,
        String category,
        List<MaterialAmount> inputs,
        List<MaterialAmount> outputs,
        String machineId,
        int durationTicks,
        long energyPerTick
) {
    public RecipeDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(machineId, "machineId");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        if (category.isBlank() || machineId.isBlank()) {
            throw new IllegalArgumentException("Recipe category and machine ID must not be blank: " + id);
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one input and output: " + id);
        }
        if (durationTicks <= 0 || energyPerTick <= 0) {
            throw new IllegalArgumentException("Recipe duration and energy must be positive: " + id);
        }
    }
}