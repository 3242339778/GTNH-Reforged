package com.gtnh.reforged.materials;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable, namespaced identity for a Reforged material definition. */
public record MaterialId(String namespace, String path) implements Comparable<MaterialId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public MaterialId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid material namespace: " + namespace);
        }
        if (!PATH.matcher(path).matches() || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("Invalid material path: " + path);
        }
    }

    @Override
    public int compareTo(MaterialId other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}