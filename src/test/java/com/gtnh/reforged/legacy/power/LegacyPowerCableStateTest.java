package com.gtnh.reforged.legacy.power;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LegacyPowerCableStateTest {

    @Test
    void directNodeTakesPrecedenceOverPathOwner() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerGraphNode direct = node();
        LegacyPowerGraphNode pathOwner = node();
        LegacyPowerPath path = path(pathOwner);

        cable.setNode(direct);
        cable.setNodePath(path);

        assertSame(direct, cable.getNodeMapOwner());
    }

    @Test
    void resolvesMapOwnerThroughPathWhenCableHasNoDirectNode() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerGraphNode owner = node();
        cable.setNodePath(path(owner));

        assertSame(owner, cable.getNodeMapOwner());
    }

    @Test
    void capturesConnectionFlagsOnlyWhenPathIsBound() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        cable.setConnectionState(0b001011, true);
        LegacyPowerPath path = new LegacyPowerPath(List.of(new FakePipe()));
        cable.setNodePath(path);

        assertEquals(0b001011, cable.getNodePathConnections());

        cable.setConnectionState(0b110100, true);
        assertEquals(0b001011, cable.getNodePathConnections(),
            "later connection changes must not rewrite the path's binding snapshot");

        cable.setNodePath(null);
        assertEquals(0, cable.getNodePathConnections());

        cable.setConnectionState(0b101010, false);
        cable.setNodePath(path);
        assertEquals(0, cable.getNodePathConnections(),
            "a missing meta tile records no connection snapshot");
    }

    @Test
    void detectsEndpointOccupancyChangesEvenWhenConnectionBitsAreUnchanged() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        cable.setConnectionState(0b000101, true);
        cable.setOccupiedConnectedSides(0b000001);

        assertFalse(cable.updateOccupiedConnectedSides(0b000001));
        assertTrue(cable.updateOccupiedConnectedSides(0b000101));
        assertEquals(0b000101, cable.getOccupiedConnectedSides());
    }

    @Test
    void topologyChangeInvalidatesSharedRootWithoutImmediatelyClearingIt() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerGraphNode root = node();
        LegacyPowerGraphNode joined = new LegacyPowerGraphNode(2, new Object(), null);
        joined.joinNodeMap(root);
        cable.setNode(joined);

        cable.invalidateGraphForTopologyChange(40);

        assertFalse(root.getGraphState().isNodeMapValid());
        assertEquals(1, root.getGraphState().getTopologyVersion());
        assertEquals(40, root.getGraphState().getTopologyChangeTick());
        assertFalse(joined.isInvalid(),
            "marking a topology change invalidates the map root, not the node object");
    }

    @Test
    void immediatelyClearsDirectNodeGraph() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerGraphNode root = new LegacyPowerGraphNode(1, new Object(), cable);
        cable.setNode(root);

        cable.clearGraphImmediately();

        assertTrue(root.isInvalid());
        assertNull(cable.getNode());
    }

    @Test
    void invalidatesOnlyRemovedDirectionPathsForDirectNode() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerGraphNode root = node();
        LegacyPowerPath east = new LegacyPowerPath(List.of(new FakePipe()));
        LegacyPowerPath north = new LegacyPowerPath(List.of(new FakePipe()));
        root.setPath(LegacyPowerDirection.EAST, east);
        root.setPath(LegacyPowerDirection.NORTH, north);
        cable.setNode(root);

        Map<LegacyPowerDirection, Integer> flags = new EnumMap<>(LegacyPowerDirection.class);
        flags.put(LegacyPowerDirection.DOWN, 1);
        flags.put(LegacyPowerDirection.UP, 2);
        flags.put(LegacyPowerDirection.NORTH, 4);
        flags.put(LegacyPowerDirection.SOUTH, 8);
        flags.put(LegacyPowerDirection.WEST, 16);
        flags.put(LegacyPowerDirection.EAST, 32);

        cable.invalidateAffectedPaths(32, flags::get);

        assertFalse(east.isValid());
        assertTrue(north.isValid());
    }

    @Test
    void invalidatesIntermediatePathOnlyWhenRemovedSideWasInItsSnapshot() {
        LegacyPowerCableState cable = new LegacyPowerCableState();
        LegacyPowerPath path = new LegacyPowerPath(List.of(new FakePipe()));
        cable.setConnectionState(0b010100, true);
        cable.setNodePath(path);

        cable.invalidateAffectedPaths(0b000100, side -> 1 << side.ordinalIndex());
        assertFalse(path.isValid());

        LegacyPowerPath unaffected = new LegacyPowerPath(List.of(new FakePipe()));
        cable.setNodePath(unaffected);
        cable.invalidateAffectedPaths(0b000001, side -> 1 << side.ordinalIndex());
        assertTrue(unaffected.isValid());
    }

    private static LegacyPowerGraphNode node() {
        return new LegacyPowerGraphNode(1, new Object(), null);
    }

    private static LegacyPowerPath path(LegacyPowerPathOwner owner) {
        LegacyPowerPath path = new LegacyPowerPath(List.of(new FakePipe()));
        path.setNodeMapOwner(owner);
        return path;
    }

    private static final class FakePipe implements LegacyPowerPipeBinding {
        private LegacyPowerPath path;
        @Override public LegacyPowerPath getNodePath() { return path; }
        @Override public void setNodePath(LegacyPowerPath path) { this.path = path; }
    }
}
