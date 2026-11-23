package com.gtnh.reforged.legacy.power;

/**
 * Pure-Java decision table for one MetaPipeEntity.connect(side) attempt.
 *
 * <p>World access and mod-specific capability checks are deliberately supplied
 * as facts by the bridge. In particular, a missing neighbor block entity is
 * not enough to conclude that the target is unloaded: GT5U checks chunk
 * availability only after color, cover, compatible-pipe, and endpoint checks.</p>
 */
public final class LegacyPowerConnectionNegotiation {

    private LegacyPowerConnectionNegotiation() {}

    public enum NeighborKind {
        COMPATIBLE_PIPE,
        OTHER_OR_EMPTY
    }

    public enum Action {
        CONNECT_LOCAL_AND_RECIPROCAL_IF_NEEDED,
        CONNECT_LOCAL,
        NOT_CONNECTABLE,
        PRESERVE_EXISTING_UNLOADED
    }

    public record Decision(Action action, boolean connectReciprocal) {
        public Decision {
            if (action == null) {
                throw new NullPointerException("action");
            }
            if (connectReciprocal
                && action != Action.CONNECT_LOCAL_AND_RECIPROCAL_IF_NEEDED) {
                throw new IllegalArgumentException("Reciprocal connection requires a compatible pipe");
            }
        }

        public LegacyPowerConnectionResult asConnectionResult() {
            return switch (action) {
                case CONNECT_LOCAL, CONNECT_LOCAL_AND_RECIPROCAL_IF_NEEDED ->
                    LegacyPowerConnectionResult.CONNECTED;
                case NOT_CONNECTABLE -> LegacyPowerConnectionResult.NOT_CONNECTABLE;
                case PRESERVE_EXISTING_UNLOADED -> LegacyPowerConnectionResult.UNLOADED;
            };
        }
    }

    /**
     * Mirrors the ordering in MetaPipeEntity.connect(side).
     *
     * @param colorCompatible result of connectableColor(neighbor)
     * @param coverAllowsConnection alwaysLookConnected || letsIn || letsOut
     * @param neighborKind whether the neighbor's meta tile passes the legacy
     *                     symmetric class-compatibility test
     * @param neighborOppositeAlreadyConnected current opposite-side bit
     * @param gt6StyleConnection legacy GT6 cable setting
     * @param neighborIsAir whether the target block is air
     * @param endpointCanConnect result of cable-specific canConnect(side, tile)
     * @param targetChunkLoaded loaded-state check performed only at the same
     *                          late point as the legacy implementation
     */
    public static Decision evaluate(
        boolean colorCompatible,
        boolean coverAllowsConnection,
        NeighborKind neighborKind,
        boolean neighborOppositeAlreadyConnected,
        boolean gt6StyleConnection,
        boolean neighborIsAir,
        boolean endpointCanConnect,
        boolean targetChunkLoaded) {

        if (!colorCompatible || !coverAllowsConnection) {
            return new Decision(Action.NOT_CONNECTABLE, false);
        }

        if (neighborKind == NeighborKind.COMPATIBLE_PIPE) {
            return new Decision(
                Action.CONNECT_LOCAL_AND_RECIPROCAL_IF_NEEDED,
                !neighborOppositeAlreadyConnected);
        }

        if ((gt6StyleConnection && neighborIsAir) || endpointCanConnect) {
            return new Decision(Action.CONNECT_LOCAL, false);
        }

        if (!targetChunkLoaded) {
            return new Decision(Action.PRESERVE_EXISTING_UNLOADED, false);
        }

        return new Decision(Action.NOT_CONNECTABLE, false);
    }

    /**
     * Mirrors the compatible-pipe predicate:
     * thisClass.isInstance(other) || otherClass.isInstance(this). For class
     * tokens, the equivalent test is symmetric isAssignableFrom().
     */
    public static boolean compatiblePipeClasses(
        Class<?> thisPipeClass,
        Class<?> otherPipeClass) {
        if (thisPipeClass == null || otherPipeClass == null) {
            return false;
        }
        return thisPipeClass.isAssignableFrom(otherPipeClass)
            || otherPipeClass.isAssignableFrom(thisPipeClass);
    }

    /**
     * Mirrors disconnect(side)'s reciprocal disconnect eligibility. The
     * caller still performs the actual remote callback.
     */
    public static boolean shouldDisconnectReciprocal(
        boolean compatiblePipeClasses,
        boolean neighborOppositeAlreadyConnected) {
        return compatiblePipeClasses && neighborOppositeAlreadyConnected;
    }
}
