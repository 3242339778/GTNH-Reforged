package com.gtnh.reforged.legacy.power;

/** Host binding for the node reference stored on a legacy pipe tile. */
public interface LegacyPowerNodeBinding {

    LegacyPowerGraphNode getNode();

    void setNode(LegacyPowerGraphNode node);
}
