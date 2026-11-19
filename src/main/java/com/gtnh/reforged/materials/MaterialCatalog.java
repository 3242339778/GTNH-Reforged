package com.gtnh.reforged.materials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable, deterministically ordered collection of validated material definitions. */
public final class MaterialCatalog {
    private final NavigableMap<MaterialId, MaterialDefinition> definitions;

    public MaterialCatalog(Iterable<MaterialDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        NavigableMap<MaterialId, MaterialDefinition> indexed = new TreeMap<>();
        for (MaterialDefinition definition : definitions) {
            Objects.requireNonNull(definition, "material definition");
            MaterialDefinition previous = indexed.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate material ID: " + definition.id());
            }
        }
        this.definitions = Collections.unmodifiableNavigableMap(indexed);
    }

    public Optional<MaterialDefinition> find(MaterialId id) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id")));
    }

    public List<MaterialDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public List<MaterialDefinition> withForm(MaterialForm form) {
        Objects.requireNonNull(form, "form");
        List<MaterialDefinition> matches = new ArrayList<>();
        for (MaterialDefinition definition : definitions.values()) {
            if (definition.forms().contains(form)) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    public int size() {
        return definitions.size();
    }

    public Map<MaterialId, MaterialDefinition> asMap() {
        return definitions;
    }
}