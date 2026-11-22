package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-independent extraction of GTNH's Lock collection semantics.
 *
 * <p>Entries are compared by object identity, not equals(). A null entry is
 * permitted because the upstream overload path calls addTileEntity(null).
 * This class is not thread-safe, matching the logical-server-thread usage of
 * the legacy graph.</p>
 */
public final class LegacyPowerPathLock {

    private final List<Object> owners = new ArrayList<>();

    public void addOwner(Object owner) {
        if (indexOfIdentity(owner) < 0) {
            owners.add(owner);
        }
    }

    public void removeOwner(Object owner) {
        int index = indexOfIdentity(owner);
        if (index >= 0) {
            owners.remove(index);
        }
    }

    public boolean isLocked() {
        return !owners.isEmpty();
    }

    /** Returns the number of distinct owner identities, including null. */
    public int ownerCount() {
        return owners.size();
    }

    private int indexOfIdentity(Object owner) {
        for (int i = 0; i < owners.size(); i++) {
            if (owners.get(i) == owner) {
                return i;
            }
        }
        return -1;
    }
}
