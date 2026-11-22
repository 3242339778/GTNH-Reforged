package com.gtnh.reforged.legacy.power;

import java.util.List;
import java.util.Objects;

/** Cursor and stale marker matching GTNH 5.09.54.275 NodeList. */
public final class LegacyPowerConsumerList {
    private final List<? extends LegacyPowerConsumerAccess> consumers;
    private int position;
    private boolean stale;

    public LegacyPowerConsumerList(List<? extends LegacyPowerConsumerAccess> consumers) {
        this.consumers = List.copyOf(Objects.requireNonNull(consumers, "consumers"));
    }

    public LegacyPowerConsumerAccess getNode() {
        return position < consumers.size() ? consumers.get(position) : null;
    }

    public LegacyPowerConsumerAccess getNextNode() {
        position++;
        return getNode();
    }

    public int getPosition() {
        return position;
    }

    public void markStale() {
        stale = true;
    }

    public boolean isStale() {
        return stale;
    }
}
