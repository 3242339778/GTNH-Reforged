package com.gtnh.reforged.legacy.power;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerPathTest {

    @Test
    void constructionBindsEveryPipeToTheNewPath() {
        FakePipe first = new FakePipe();
        FakePipe second = new FakePipe();

        LegacyPowerPath path = new LegacyPowerPath(List.of(first, second));

        assertSame(path, first.getNodePath());
        assertSame(path, second.getNodePath());
        assertTrue(path.isValid());
    }

    @Test
    void clearPathDetachesOnlyPipesStillBoundToThisPath() {
        FakePipe retained = new FakePipe();
        FakePipe rebound = new FakePipe();
        LegacyPowerPath path = new LegacyPowerPath(List.of(retained, rebound));
        LegacyPowerPath replacement = new LegacyPowerPath(List.of(rebound));

        path.clearPath();

        assertNull(retained.getNodePath());
        assertSame(replacement, rebound.getNodePath());
        assertFalse(path.isValid());
        assertNull(path.getNodeMapOwner());
    }

    @Test
    void invalidatingUnownedPathClearsIt() {
        FakePipe pipe = new FakePipe();
        LegacyPowerPath path = new LegacyPowerPath(List.of(pipe));

        path.invalidateNodeMap();

        assertFalse(path.isValid());
        assertNull(pipe.getNodePath());
    }

    @Test
    void invalidatingOwnedPathDelegatesFullGraphClear() {
        FakePipe pipe = new FakePipe();
        LegacyPowerPath path = new LegacyPowerPath(List.of(pipe));
        int[] calls = { 0 };
        int[] returnValues = { 0 };
        LegacyPowerPathOwner owner = value -> {
            calls[0]++;
            returnValues[0] = value;
        };
        path.setNodeMapOwner(owner);

        path.invalidateNodeMap();

        assertEquals(1, calls[0]);
        assertEquals(-1, returnValues[0]);
        assertTrue(path.isValid());
        assertSame(path, pipe.getNodePath());
        assertSame(owner, path.getNodeMapOwner());
    }

    @Test
    void invalidateDoesNotDetachPipeBindings() {
        FakePipe pipe = new FakePipe();
        LegacyPowerPath path = new LegacyPowerPath(List.of(pipe));

        path.invalidate();

        assertFalse(path.isValid());
        assertSame(path, pipe.getNodePath());
    }

    @Test
    void lockUsesIdentityAndAllowsOnlyOneNullOwner() {
        Object first = new EqualObject();
        Object equalButDistinct = new EqualObject();
        LegacyPowerPathLock lock = new LegacyPowerPathLock();

        lock.addOwner(first);
        lock.addOwner(first);
        lock.addOwner(equalButDistinct);
        lock.addOwner(null);
        lock.addOwner(null);

        assertTrue(lock.isLocked());
        assertEquals(3, lock.ownerCount());

        lock.removeOwner(new EqualObject());
        assertEquals(3, lock.ownerCount());
        lock.removeOwner(first);
        assertEquals(2, lock.ownerCount());
        lock.removeOwner(null);
        assertEquals(1, lock.ownerCount());
    }

    private static final class FakePipe implements LegacyPowerPipeBinding {

        private LegacyPowerPath nodePath;

        @Override
        public LegacyPowerPath getNodePath() {
            return nodePath;
        }

        @Override
        public void setNodePath(LegacyPowerPath path) {
            nodePath = path;
        }
    }

    private static final class EqualObject {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualObject;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
