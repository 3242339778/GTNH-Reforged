package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft-independent extraction of GenerateNodeMap.getNextValidTileEntity
 * from GT5-Unofficial 5.09.54.275.
 *
 * <p>This class intentionally implements only the iterative degree-two walk.
 * Branch expansion, node creation, path installation, and graph clearing are
 * separate operations and are not implied by this class.</p>
 */
public final class LegacyPowerTopologyWalker {

    private final LegacyPowerTopologyAccess access;

    public LegacyPowerTopologyWalker(LegacyPowerTopologyAccess access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    /**
     * Walks from a neighboring tile toward the next graph node.
     *
     * @param firstTile tile adjacent to the current node
     * @param side direction from the previous pipe toward {@code firstTile}
     * @return endpoint and intermediate pipes, or null when upstream returns
     *         null because the route terminates at an already-owned node,
     *         disconnected incoming side, or exhausted connected directions
     */
    public WalkResult walk(Object firstTile, LegacyPowerDirection side) {
        Objects.requireNonNull(side, "side");
        Object tile = firstTile;
        LegacyPowerDirection enteredFrom = side;
        List<Object> intermediatePipes = new ArrayList<>();

        walk:
        while (true) {
            if (tile == null) {
                return null;
            }
            if (!access.isPipe(tile)) {
                return new WalkResult(tile, enteredFrom, intermediatePipes);
            }

            Object node = access.getNode(tile);
            if (node != null && access.containsNode(node)) {
                return null;
            }

            LegacyPowerDirection previousSide = enteredFrom.opposite();
            if (!access.isConnected(tile, previousSide)) {
                return null;
            }

            Object owner = access.getNodeMapOwner(tile);
            if (owner != null) {
                if (access.containsMapOwner(owner)) {
                    return null;
                }
                // Upstream retires the other graph before replacing bindings.
                access.clearNodeMap(owner, -1);
            }

            int connections = countConnections(tile);
            if (connections != 2 || access.isEnergySource(tile)) {
                return new WalkResult(tile, enteredFrom, intermediatePipes);
            }

            for (LegacyPowerDirection direction : LegacyPowerDirection.orderedValues()) {
                if (direction == previousSide || !access.isConnected(tile, direction)) {
                    continue;
                }
                Object next = access.getNeighbor(tile, direction);
                if (next == null) {
                    return new WalkResult(tile, enteredFrom, intermediatePipes);
                }
                if (access.isPipe(next)) {
                    intermediatePipes.add(tile);
                    tile = next;
                    enteredFrom = direction;
                    continue walk;
                }
                return new WalkResult(tile, enteredFrom, intermediatePipes);
            }
            return null;
        }
    }

    private int countConnections(Object pipe) {
        int count = 0;
        for (LegacyPowerDirection direction : LegacyPowerDirection.orderedValues()) {
            if (access.isConnected(pipe, direction)) {
                count++;
            }
        }
        return count;
    }

    public record WalkResult(Object endpoint, LegacyPowerDirection enteredFrom, List<Object> intermediatePipes) {
        public WalkResult {
            Objects.requireNonNull(endpoint, "endpoint");
            Objects.requireNonNull(enteredFrom, "enteredFrom");
            intermediatePipes = List.copyOf(intermediatePipes);
        }
    }
}
