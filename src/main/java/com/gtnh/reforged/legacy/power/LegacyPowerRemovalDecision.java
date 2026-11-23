package com.gtnh.reforged.legacy.power;

/**
 * Conservative classification of a BlockEntity removal observed by the
 * Minecraft-facing host.
 *
 * <p>This model deliberately distinguishes a block replacement from removal
 * of a host instance while the cable block remains present. Missing world or
 * chunk information is UNKNOWN and must never trigger graph cleanup.</p>
 */
public final class LegacyPowerRemovalDecision {

    public enum Result {
        CHUNK_UNLOAD,
        BLOCK_REPLACED,
        HOST_REPLACED,
        UNKNOWN
    }

    private LegacyPowerRemovalDecision() {
    }

    /**
     * Classifies a setRemoved callback using facts collected by the Minecraft
     * bridge without loading a chunk.
     *
     * @param chunkUnloadPending whether onChunkUnloaded was observed first
     * @param worldStateAvailable whether the containing chunk was already loaded
     * @param currentBlockIsCable whether the block currently stored at the
     *                             position is still the prototype cable block
     * @param currentBlockEntityIsThis whether the chunk still maps the position
     *                                 to this exact BlockEntity instance
     */
    public static Result classify(
        boolean chunkUnloadPending,
        boolean worldStateAvailable,
        boolean currentBlockIsCable,
        boolean currentBlockEntityIsThis) {

        if (chunkUnloadPending) {
            return Result.CHUNK_UNLOAD;
        }
        if (!worldStateAvailable) {
            return Result.UNKNOWN;
        }
        if (!currentBlockIsCable) {
            return Result.BLOCK_REPLACED;
        }
        if (!currentBlockEntityIsThis) {
            return Result.HOST_REPLACED;
        }
        return Result.UNKNOWN;
    }
}
