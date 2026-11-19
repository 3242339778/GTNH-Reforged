package com.gtnh.reforged.recipes;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable namespaced identity for a processing recipe. */
public record RecipeId(String namespace, String path) implements Comparable<RecipeId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public RecipeId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()
                || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("Invalid recipe ID: " + namespace + ":" + path);
        }
    }

    @Override
    public int compareTo(RecipeId other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}