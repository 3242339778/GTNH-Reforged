package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerTopologyWalkerTest {

    private final FakeWorld world = new FakeWorld();
    private final LegacyPowerTopologyWalker walker = new LegacyPowerTopologyWalker(world);

    @Test
    void collapsesStraightDegreeTwoRunIntoIntermediatePath() {
        Pipe first = world.pipe();
        Pipe second = world.pipe();
        Tile machine = new Tile();
        world.connect(first, LegacyPowerDirection.EAST, new Tile(), LegacyPowerDirection.WEST);
        world.connect(first, LegacyPowerDirection.WEST, second, LegacyPowerDirection.EAST);
        world.connect(second, LegacyPowerDirection.WEST, machine, LegacyPowerDirection.EAST);

        LegacyPowerTopologyWalker.WalkResult result =
            walker.walk(first, LegacyPowerDirection.WEST);

        assertSame(second, result.endpoint());
        assertEquals(LegacyPowerDirection.WEST, result.enteredFrom());
        assertEquals(List.of(first), result.intermediatePipes());
    }

    @Test
    void keepsDegreeTwoEnergySourceAsEndpointNode() {
        Pipe source = world.pipe();
        world.connect(source, LegacyPowerDirection.EAST, new Tile(), LegacyPowerDirection.WEST);
        world.connect(source, LegacyPowerDirection.WEST, new Tile(), LegacyPowerDirection.EAST);
        source.energySource = true;

        LegacyPowerTopologyWalker.WalkResult result =
            walker.walk(source, LegacyPowerDirection.WEST);

        assertSame(source, result.endpoint());
        assertTrue(result.intermediatePipes().isEmpty());
    }

    @Test
    void stopsWhenPipeNodeAlreadyBelongsToCurrentMap() {
        Pipe pipe = world.pipe();
        Object existingNode = new Object();
        pipe.node = existingNode;
        world.currentNodes.add(existingNode);

        assertNull(walker.walk(pipe, LegacyPowerDirection.WEST));
        assertTrue(world.clearedOwners.isEmpty());
    }

    @Test
    void clearsForeignMapBeforeWalkingThroughItsPipe() {
        Pipe foreign = world.pipe();
        Pipe next = world.pipe();
        Tile endpoint = new Tile();
        Object foreignOwner = new Object();
        foreign.owner = foreignOwner;
        world.connect(foreign, LegacyPowerDirection.EAST, new Tile(), LegacyPowerDirection.WEST);
        world.connect(foreign, LegacyPowerDirection.WEST, next, LegacyPowerDirection.EAST);
        world.connect(next, LegacyPowerDirection.WEST, endpoint, LegacyPowerDirection.EAST);

        LegacyPowerTopologyWalker.WalkResult result =
            walker.walk(foreign, LegacyPowerDirection.WEST);

        assertSame(next, result.endpoint());
        assertEquals(List.of(foreign), result.intermediatePipes());
        assertEquals(List.of(foreignOwner), world.clearedOwners);
        assertEquals(List.of(-1), world.clearReturnValues);
    }

    @Test
    void returnsCurrentPipeWhenNextConnectedTileIsMissing() {
        Pipe pipe = world.pipe();
        world.connect(pipe, LegacyPowerDirection.EAST, new Tile(), LegacyPowerDirection.WEST);
        world.connect(pipe, LegacyPowerDirection.WEST, null, LegacyPowerDirection.EAST);

        LegacyPowerTopologyWalker.WalkResult result =
            walker.walk(pipe, LegacyPowerDirection.WEST);

        assertSame(pipe, result.endpoint());
        assertTrue(result.intermediatePipes().isEmpty());
    }

    @Test
    void disconnectedIncomingSideTerminatesWithoutEndpoint() {
        Pipe pipe = world.pipe();
        world.connect(pipe, LegacyPowerDirection.WEST, new Tile(), LegacyPowerDirection.EAST);

        assertNull(walker.walk(pipe, LegacyPowerDirection.WEST));
    }

    private static class Tile {
    }

    private static final class Pipe extends Tile {
        private final Map<LegacyPowerDirection, Object> neighbors =
            new EnumMap<>(LegacyPowerDirection.class);
        private final Set<LegacyPowerDirection> connected =
            new HashSet<>();
        private Object node;
        private Object owner;
        private boolean energySource;
    }

    private static final class FakeWorld implements LegacyPowerTopologyAccess {
        private final Set<Object> currentNodes = new HashSet<>();
        private final Set<Object> currentOwners = new HashSet<>();
        private final List<Object> clearedOwners = new ArrayList<>();
        private final List<Integer> clearReturnValues = new ArrayList<>();

        Pipe pipe() {
            return new Pipe();
        }

        void connect(Pipe first, LegacyPowerDirection firstSide,
            Object second, LegacyPowerDirection secondSide) {
            first.connected.add(firstSide);
            first.neighbors.put(firstSide, second);
            if (second instanceof Pipe secondPipe) {
                secondPipe.connected.add(secondSide);
                secondPipe.neighbors.put(secondSide, first);
            }
        }

        @Override
        public boolean isPipe(Object tile) {
            return tile instanceof Pipe;
        }

        @Override
        public Object getNode(Object pipe) {
            return ((Pipe) pipe).node;
        }

        @Override
        public Object getNodeMapOwner(Object pipe) {
            return ((Pipe) pipe).owner;
        }

        @Override
        public boolean isConnected(Object pipe, LegacyPowerDirection side) {
            return ((Pipe) pipe).connected.contains(side);
        }

        @Override
        public Object getNeighbor(Object pipe, LegacyPowerDirection side) {
            return ((Pipe) pipe).neighbors.get(side);
        }

        @Override
        public boolean containsNode(Object node) {
            return currentNodes.contains(node);
        }

        @Override
        public boolean containsMapOwner(Object owner) {
            return currentOwners.contains(owner);
        }

        @Override
        public void clearNodeMap(Object owner, int returnNodeValue) {
            clearedOwners.add(owner);
            clearReturnValues.add(returnNodeValue);
        }

        @Override
        public boolean isEnergySource(Object pipe) {
            return ((Pipe) pipe).energySource;
        }
    }
}
