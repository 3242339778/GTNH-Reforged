package com.gtnh.reforged.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.HashMap;

import com.gtnh.reforged.materials.MaterialForm;
import com.gtnh.reforged.materials.MaterialId;

/** Immutable recipe snapshot with prebuilt reverse-lookup indexes. */
public final class RecipeCatalog {
    private final NavigableMap<RecipeId, RecipeDefinition> definitions;
    private final Map<MaterialFormKey, List<RecipeDefinition>> byInput;
    private final Map<MaterialFormKey, List<RecipeDefinition>> byOutput;
    private final Map<String, List<RecipeDefinition>> byMachine;

    public RecipeCatalog(Iterable<RecipeDefinition> recipes) {
        Objects.requireNonNull(recipes, "recipes");
        NavigableMap<RecipeId, RecipeDefinition> indexed = new TreeMap<>();
        Map<MaterialFormKey, List<RecipeDefinition>> inputIndex = new HashMap<>();
        Map<MaterialFormKey, List<RecipeDefinition>> outputIndex = new HashMap<>();
        Map<String, List<RecipeDefinition>> machineIndex = new HashMap<>();
        for (RecipeDefinition recipe : recipes) {
            Objects.requireNonNull(recipe, "recipe");
            if (indexed.putIfAbsent(recipe.id(), recipe) != null) {
                throw new IllegalArgumentException("Duplicate recipe ID: " + recipe.id());
            }
            recipe.inputs().forEach(stack -> inputIndex
                    .computeIfAbsent(new MaterialFormKey(stack.material(), stack.form()), ignored -> new ArrayList<>())
                    .add(recipe));
            recipe.outputs().forEach(stack -> outputIndex
                    .computeIfAbsent(new MaterialFormKey(stack.material(), stack.form()), ignored -> new ArrayList<>())
                    .add(recipe));
            machineIndex.computeIfAbsent(recipe.machineId(), ignored -> new ArrayList<>()).add(recipe);
        }
        this.definitions = Collections.unmodifiableNavigableMap(indexed);
        this.byInput = freeze(inputIndex);
        this.byOutput = freeze(outputIndex);
        this.byMachine = freeze(machineIndex);
    }

    public Optional<RecipeDefinition> find(RecipeId id) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id")));
    }

    public List<RecipeDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public List<RecipeDefinition> accepting(MaterialId material, MaterialForm form) {
        return byInput.getOrDefault(new MaterialFormKey(material, form), List.of());
    }

    public List<RecipeDefinition> producing(MaterialId material, MaterialForm form) {
        return byOutput.getOrDefault(new MaterialFormKey(material, form), List.of());
    }

    public List<RecipeDefinition> forMachine(String machineId) {
        return byMachine.getOrDefault(Objects.requireNonNull(machineId, "machineId"), List.of());
    }

    public int size() {
        return definitions.size();
    }

    private static <K> Map<K, List<RecipeDefinition>> freeze(Map<K, List<RecipeDefinition>> source) {
        Map<K, List<RecipeDefinition>> frozen = new HashMap<>();
        source.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        return Map.copyOf(frozen);
    }

    private record MaterialFormKey(MaterialId material, MaterialForm form) {
        private MaterialFormKey {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(form, "form");
        }
    }
}