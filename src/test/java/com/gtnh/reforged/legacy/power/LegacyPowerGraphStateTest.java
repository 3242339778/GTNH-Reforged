package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerGraphStateTest {

    @Test
    void joinedNodesObserveInvalidationThroughSharedRoot() {
        LegacyPowerGraphState root = new LegacyPowerGraphState();
        LegacyPowerGraphState branch = new LegacyPowerGraphState();
        branch.joinNodeMap(root);

        branch.invalidateNodeMap(120L);

        assertFalse(root.isNodeMapValid());
        assertFalse(branch.isNodeMapValid());
        assertEquals(1L, root.getTopologyVersion());
        assertEquals(1L, branch.getTopologyVersion());
        assertEquals(120L, root.getTopologyChangeTick());
    }

    @Test
    void refreshIsNotDueBeforeTenTicksAndIsDueAtTenTicks() {
        LegacyPowerGraphState graph = new LegacyPowerGraphState();
        graph.invalidateNodeMap(500L);

        assertFalse(graph.isNodeMapRefreshDue(500L));
        assertFalse(graph.isNodeMapRefreshDue(509L));
        assertTrue(graph.isNodeMapRefreshDue(510L));
        assertTrue(graph.isNodeMapRefreshDue(511L));
    }

    @Test
    void eachInvalidationIncrementsVersionAndResetsDelayWindow() {
        LegacyPowerGraphState graph = new LegacyPowerGraphState();
        graph.invalidateNodeMap(10L);
        graph.invalidateNodeMap(17L);

        assertEquals(2L, graph.getTopologyVersion());
        assertEquals(17L, graph.getTopologyChangeTick());
        assertFalse(graph.isNodeMapRefreshDue(26L));
        assertTrue(graph.isNodeMapRefreshDue(27L));
    }

    @Test
    void joiningUsesPreviousNodesRootRatherThanMergingTwoRoots() {
        LegacyPowerGraphState firstRoot = new LegacyPowerGraphState();
        LegacyPowerGraphState secondRoot = new LegacyPowerGraphState();
        LegacyPowerGraphState firstChild = new LegacyPowerGraphState();
        LegacyPowerGraphState secondChild = new LegacyPowerGraphState();

        firstChild.joinNodeMap(firstRoot);
        secondChild.joinNodeMap(secondRoot);

        // Upstream joinNodeMap assigns the previous node's root to this node.
        // It does not merge both existing maps or rewrite other members.
        secondChild.joinNodeMap(firstChild);

        assertSame(firstRoot, secondChild.getNodeMapRoot());
        assertSame(secondRoot, secondRoot.getNodeMapRoot());
        assertTrue(secondRoot.isNodeMapValid());

        firstRoot.invalidateNodeMap(42L);
        assertFalse(secondChild.isNodeMapValid());
        assertTrue(secondRoot.isNodeMapValid());
    }

    @Test
    void nullJoinDoesNotChangeCurrentRoot() {
        LegacyPowerGraphState graph = new LegacyPowerGraphState();
        graph.joinNodeMap(null);

        assertSame(graph, graph.getNodeMapRoot());
        assertTrue(graph.isNodeMapValid());
    }

    @Test
    void earlierTickDoesNotAccidentallySatisfyRefreshDelay() {
        LegacyPowerGraphState graph = new LegacyPowerGraphState();
        graph.invalidateNodeMap(100L);

        assertFalse(graph.isNodeMapRefreshDue(90L));
    }
}
