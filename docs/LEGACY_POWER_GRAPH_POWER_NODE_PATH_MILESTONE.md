# Legacy Power Node Path — Milestone

## Baseline

Implementation target: GT5-Unofficial **5.09.54.275**.

Primary upstream sources:

- [PowerNodePath.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/paths/PowerNodePath.java)
- [NodePath.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/paths/NodePath.java)
- [AveragePerTickCounter.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/util/AveragePerTickCounter.java)
- [CableReadout.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/paths/CableReadout.java)

## Implemented

- `LegacyPowerCableAccess`: Minecraft-independent cable rating, loss, base-tile availability, and fire callback contract.
- `LegacyPowerNodePath`: specializes `LegacyPowerPath` and retains:
  - minimum cable amperage and voltage ratings;
  - sum of cable loss values;
  - post-loss voltage max-sampling per tick;
  - amperage and energy sum-sampling per tick;
  - upstream voltage selection condition and tick-based current decay;
  - path-wide overload sentinel (null lock owner);
  - per-cable voltage/amperage fire thresholds;
  - previous-tick and preceding-20-tick readouts.
- `LegacyCableReadout`: immutable snapshot matching the upstream eight-field readout shape.
- `LegacyAveragePerTickCounter` is reused for the three counters.

## Preserved behavior details

- Voltage counter values are post-loss and use max-per-tick semantics.
- `mVoltage` tracks the selected raw input voltage; it is not the same value as the post-loss voltage counter.
- Amperage and energy are summed per tick; energy uses `amps * max(voltageAfterLoss, 0)`.
- Current accumulation decays by `maxAmps * ticksPassed`; negative or more than 100 elapsed ticks resets it to zero.
- Overload threshold is strictly greater than `maxAmps * 40`.
- Overvoltage is detected when input voltage is strictly greater than the path's minimum cable voltage rating.
- Fire is requested only for individual cables whose own rating is below the selected path voltage/current and whose base tile is present.
- Both overload paths add a null sentinel to the shared path lock.
- Readout maximum voltage is `max(0, minCableVoltage - totalPathLoss)`; maximum EU/t is that value multiplied by minimum cable amperage.
- `getReadout()` is non-mutating.

## Adapter obligations

A NeoForge integration must implement `LegacyPowerCableAccess` against the actual GTNH-compatible cable and base-tile objects. In particular:

- Return the exact cable amperage, voltage, and loss fields used by GTNH 5.09.54.275.
- `hasBaseTile()` must reflect whether the old base tile entity lookup returns non-null.
- `setToFire()` must invoke the corresponding legacy fire behavior, not merely set a visual flag.
- Tick supplier must return the same logical server tick counter used by the graph.

## Scope boundary

This milestone does not implement:

- graph traversal or node construction;
- `PowerNodes` consumer selection or recursive transfer;
- packet/amperage distribution and partial-transfer recovery;
- actual NeoForge block entities, fire scheduling, scanner UI, or Waila UI.

Those remain separate integration and behavior-test work.

## Local verification

Run:

```powershell
.\\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerNodePathTest" --rerun
.\\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

The implementation is not considered locally verified until both commands pass on the development machine.
