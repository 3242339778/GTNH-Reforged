package com.gtnh.reforged.legacy.power;

/**
 * Pure-Java policy extracted from GT5-Unofficial 5.09.54.275
 * MetaPipeEntity.setCheckConnections()/checkConnections() and the two cable
 * tick paths in BaseMetaPipeEntity and MTECable.
 *
 * <p>This class decides when a check is dispatched and which directions the
 * legacy check is allowed to inspect. World lookup, cover policy, color
 * compatibility, endpoint capability checks, reciprocal pipe calls, and
 * graph invalidation remain responsibilities of the bridge.</p>
 */
public final class LegacyPowerConnectionCheckPolicy {

    private LegacyPowerConnectionCheckPolicy() {}

    public enum Dispatch {
        CLIENT_IGNORED,
        MANAGED_CABLE_QUEUE,
        IMMEDIATE,
        DEFERRED_TO_META_TILE_TICK
    }

    /**
     * Mirrors MetaPipeEntity.setCheckConnections().
     */
    public static Dispatch request(
        boolean serverSide,
        boolean nonTickingBuiltinCable,
        boolean tickDisabled,
        boolean deferCheckConnection) {
        if (!serverSide) {
            return Dispatch.CLIENT_IGNORED;
        }
        if (nonTickingBuiltinCable) {
            return Dispatch.MANAGED_CABLE_QUEUE;
        }
        if (tickDisabled || !deferCheckConnection) {
            return Dispatch.IMMEDIATE;
        }
        return Dispatch.DEFERRED_TO_META_TILE_TICK;
    }

    /**
     * Mirrors the normal ticking MTECable.onPostTick() condition. Addon cable
     * subclasses use this path; built-in cables are handled by the manager.
     */
    public static boolean shouldRunMetaTileTick(
        boolean serverSide,
        long metaTileTick,
        boolean checkPending,
        boolean gt6StyleConnection) {
        return serverSide
            && metaTileTick % 20L == 0L
            && (checkPending || !gt6StyleConnection);
    }

    /**
     * Mirrors BaseMetaPipeEntity.tickManagedCable(). A pending request runs
     * immediately on the next managed-cable manager pass; traditional
     * auto-connect cables also refresh every twentieth world tick.
     */
    public static boolean shouldRunManagedCableTick(
        boolean checkPending,
        boolean gt6StyleConnection,
        long worldTick) {
        return checkPending || (!gt6StyleConnection && worldTick % 20L == 0L);
    }

    /**
     * Traditional mode attempts all six directions. GT6-style mode only
     * revalidates directions already marked connected, so it never discovers
     * a new open side by itself.
     */
    public static boolean shouldAttemptDirection(
        boolean gt6StyleConnection,
        int currentConnectionMask,
        int sideFlag) {
        requireSideFlag(sideFlag);
        return !gt6StyleConnection || (currentConnectionMask & sideFlag) != 0;
    }

    /**
     * Applies one attempted side's tri-state result. An unattempted GT6-style
     * side and an unloaded target both preserve the previous bit.
     */
    public static int applyDirectionResult(
        boolean gt6StyleConnection,
        int currentConnectionMask,
        int sideFlag,
        LegacyPowerConnectionResult result) {
        if (!shouldAttemptDirection(gt6StyleConnection, currentConnectionMask, sideFlag)) {
            return currentConnectionMask;
        }
        return result.updateConnectionMask(currentConnectionMask, sideFlag);
    }

    private static void requireSideFlag(int sideFlag) {
        if (sideFlag == 0) {
            throw new IllegalArgumentException("A physical side flag must be non-zero");
        }
    }
}
