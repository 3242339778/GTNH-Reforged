package com.gtnh.reforged.legacy.power;

/**
 * Optional cable capabilities consumed by the legacy power-path model.
 * Non-cable path bindings implement only LegacyPowerPipeBinding and are
 * ignored when PowerNodePath calculates cable-derived limits.
 */
public interface LegacyPowerCableAccess extends LegacyPowerPipeBinding {
    long getAmperageRating();
    long getVoltageRating();
    long getCableLossPerMeter();
    boolean hasBaseTile();
    void setToFire();
}
