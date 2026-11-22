package com.gtnh.reforged.legacy.power;

import java.util.List;
import java.util.Objects;

/**
 * Minecraft-independent extraction of GTNH 5.09.54.275 NodePath ownership
 * and cleanup behavior.
 *
 * <p>This class models path-to-pipe bindings, optional graph ownership,
 * validity, and the path lock. PowerNodePath-specific electrical behavior is
 * implemented by LegacyPowerNodePath.</p>
 */
public class LegacyPowerPath {

    private final List<LegacyPowerPipeBinding> pipes;
    private LegacyPowerPathLock lock = new LegacyPowerPathLock();
    private LegacyPowerPathOwner nodeMapOwner;
    private boolean valid = true;

    public LegacyPowerPath(List<? extends LegacyPowerPipeBinding> pipes) {
        Objects.requireNonNull(pipes, "pipes");
        this.pipes = List.copyOf(pipes);
        for (LegacyPowerPipeBinding pipe : this.pipes) {
            Objects.requireNonNull(pipe, "pipe");
            pipe.setNodePath(this);
        }
    }

    public List<LegacyPowerPipeBinding> getPipes() {
        return pipes;
    }

    public void setNodeMapOwner(LegacyPowerPathOwner owner) {
        this.nodeMapOwner = owner;
    }

    public LegacyPowerPathOwner getNodeMapOwner() {
        return nodeMapOwner;
    }

    /**
     * Mirrors upstream NodePath.invalidateNodeMap(): an unowned path clears
     * itself; an owned path delegates clearing to its owning graph.
     */
    public void invalidateNodeMap() {
        LegacyPowerPathOwner owner = nodeMapOwner;
        if (owner == null) {
            clearPath();
        } else {
            owner.clearNodeMap(-1);
        }
    }

    public boolean isValid() {
        return valid;
    }

    /** Marks the path invalid without detaching pipe bindings. */
    public void invalidate() {
        valid = false;
    }

    /**
     * Invalidates this path, drops graph ownership, and detaches only pipes
     * that still point to this exact path instance.
     */
    public void clearPath() {
        valid = false;
        nodeMapOwner = null;
        for (LegacyPowerPipeBinding pipe : pipes) {
            if (pipe.getNodePath() == this) {
                pipe.setNodePath(null);
            }
        }
    }

    public LegacyPowerPathLock getLock() {
        return lock;
    }

    /** Installs the shared endpoint lock used by the legacy graph. */
    public void setLock(LegacyPowerPathLock lock) {
        this.lock = Objects.requireNonNull(lock, "lock");
    }
}
