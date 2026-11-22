package com.gtnh.reforged.legacy.power;

/**
 * Host callback for the upstream NodePath.invalidateNodeMap() behavior.
 *
 * <p>In GTNH 5.09.54.275 this callback corresponds to
 * GenerateNodeMap.clearNodeMap(node, -1), not merely flipping the shared
 * topology-valid flag.</p>
 */
@FunctionalInterface
public interface LegacyPowerPathOwner {

    void clearNodeMap(int returnNodeValue);
}
