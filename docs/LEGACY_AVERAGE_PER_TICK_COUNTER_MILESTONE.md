# Legacy Average Per Tick Counter — Milestone

## Baseline

Extracted from GT5-Unofficial `5.09.54.275`, `gregtech.api.util.AveragePerTickCounter`.

## Implementation

`LegacyAveragePerTickCounter` is a pure-Java extraction with an injected `IntSupplier` tick source. It preserves:
- period validation and a ring buffer of `period + 1`;
- same-tick sum mode and same-tick max mode;
- advancement only when the observed tick is strictly greater than the current slot timestamp;
- zero-value suppression;
- `getLast()` matching exactly the previous tick;
- `getAverage()` over [currentTick - period, currentTick), excluding the current tick;
- per-tick average caching;
- the upstream signed 32-bit tick representation and backward-tick behavior.

The implementation deliberately does not add saturation, overflow guards, tick rollback normalization, or alternate averaging semantics.

## Verification

Run:
```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyAveragePerTickCounterTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

## Scope boundary

This milestone extracts only the rolling counter. It does not yet implement `PowerNodePath`, cable ratings, voltage loss, overload locks, fire behavior, or power transfer.
