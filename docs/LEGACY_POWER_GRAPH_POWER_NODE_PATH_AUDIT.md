# Legacy Power Graph — PowerNodePath Behavior Audit

## Baseline

GT5-Unofficial tag: `5.09.54.275`

Audited upstream sources:
- `gregtech.api.graphs.paths.PowerNodePath`
- `gregtech.api.graphs.paths.NodePath`
- `gregtech.api.graphs.PowerNodes`
- `gregtech.api.util.AveragePerTickCounter`
- `gregtech.api.enums.TickTime`
- relevant `MTECable` callers and cable rating fields

## 1. Path construction and cable-derived limits

`PowerNodePath` extends `NodePath`; it must retain the base path's:
- pipe-array order and identity;
- pipe-to-path binding;
- path owner and validity;
- shared `Lock` identity and lock reload behavior.

During `processPipes()`, the power path first runs `NodePath.processPipes()`, then:
- initializes max amperage and max voltage to `Integer.MAX_VALUE`;
- takes the minimum `mAmperage` across all `MTECable` segments;
- sums every cable's `mCableLossPerMeter` into total path loss;
- takes the minimum `mVoltage` across all `MTECable` segments.

The limits are path-segment limits, not sums. Loss is summed.

## 2. Per-tick voltage behavior

State:
- `mVoltage`: current tick's selected input voltage;
- `mCountUp`: direction/counting mode associated with the selected voltage;
- `mCurrentVoltageAfterLoss`: last applied voltage after this path's loss, retained for the legacy one-argument `addAmps` overload;
- `mTick`: tick associated with the current `mVoltage`/amperage decay state.

`applyVoltage(inputVoltage, countUp)`:
1. Computes `max(inputVoltage - mLoss, 0)` and feeds it to the voltage counter using max-per-tick semantics.
2. Reads the server tick.
3. On a new tick, calls `reset(newTick - oldTick)`, updates `mTick`, stores raw input voltage, and stores `countUp`.
4. Within the same tick, changes `mCountUp` and `mVoltage` under the original boolean condition:
   `(mCountUp != countUp && inputVoltage - loss > mVoltage) || inputVoltage > mVoltage`.
5. If raw input voltage exceeds the path's minimum cable voltage rating, adds the null sentinel to the shared lock and sets fire on each cable whose own voltage rating is below the selected `mVoltage`, when its base tile still exists.

Important: the voltage counter records post-loss voltage; `mVoltage` stores the selected raw input voltage. Do not conflate these values.

## 3. Amperage decay and overload

`reset(ticksPassed)`:
- if ticksPassed is negative or greater than 100, resets `mAmps` to zero;
- otherwise subtracts `mMaxAmps * ticksPassed`, clamped at zero.

`addAmps(amps, voltageAfterLoss)`:
- adds amperage to the per-tick sum counter;
- adds `amps * max(voltageAfterLoss, 0)` to the energy counter;
- accumulates `mAmps`;
- when accumulated amperage exceeds `mMaxAmps * 40`, adds the null sentinel to the shared lock and sets fire on cable segments whose own amperage rating multiplied by 40 is below the accumulated path amperage.

The one-argument `addAmps(amps)` delegates using `mCurrentVoltageAfterLoss`. The explicit overload is used by the recursive power transfer code and must be preserved.

The `40` multiplier is an upstream behavior constant in this logic; do not silently replace it with a different tick window.

## 4. Rolling counters

Three independent `AveragePerTickCounter(TickTime.SECOND)` instances are used:
- amperage: sum all additions in the same tick;
- voltage: keep the maximum post-loss voltage in the same tick;
- energy: sum amperage multiplied by nonnegative post-loss voltage in the same tick.

`TickTime.SECOND = 20` ticks.

Counter semantics from `AveragePerTickCounter`:
- ring buffer length is period + 1;
- same-tick additions accumulate, or max-merge for voltage;
- only strictly newer ticks advance the ring index;
- `getLast()` returns the value for exactly the previous tick, otherwise zero;
- `getAverage()` sums values with timestamps in [currentTick - period, currentTick), divided by period; the current tick is excluded;
- zero additions are ignored;
- backward tick movement does not advance the ring.

This counter is a standalone reusable behavior and should be extracted/tested separately with an injected deterministic tick source.

## 5. Readout semantics

`getReadout()` is a non-mutating snapshot:
- current throughput amperage = previous tick's amperage sum;
- max amperage = path's minimum cable amperage rating;
- current voltage = previous tick's highest post-loss voltage;
- max voltage = `max(0, minCableVoltage - totalPathLoss)`;
- current energy = previous tick's energy sum;
- max energy = max voltage after loss multiplied by max amperage;
- average amperage and energy use the rolling 20-tick averages.

The current `CableReadout` does not expose average voltage, though `PowerNodePath` has an average-voltage getter.

## 6. Lock and fire interactions

Both voltage and amperage overload paths mutate the shared path lock by adding `null`. This is an identity-based lock sentinel and must remain distinct from ordinary tile-entity entries. The existing pure-Java lock prototype's single-null-owner behavior is relevant.

Overload handling also iterates the exact path cable list and checks each cable's individual rating. It is not enough to mark only the segment or only the cable that received the transfer.

## 7. PowerNodes call-order invariants

The audited `PowerNodes.processNextNode`, `processNextNodeAbove`, and `processNodeInject` call sites establish these requirements:
- check path validity and lock before applying voltage;
- apply voltage to self path and edge path before recursion/injection;
- subtract self-path loss before applying edge-path voltage;
- subtract edge-path loss before passing voltage to the next node/consumer;
- add amperage/energy statistics only after recursive transfer returns;
- self path receives the same returned amperage, with voltage adjusted by its own loss;
- consumer injection is guarded by positive remaining voltage;
- topology version is snapshotted around consumer injection; a change marks the consumer list stale;
- caller must preserve already-used amperage if it rebuilds and retries a stale consumer list.

These rules belong to the later `PowerNodes` extraction, but determine the API required by this path implementation.

## 8. Planned pure-Java extraction

Recommended order:
1. Extract deterministic `LegacyAveragePerTickCounter` with injectable tick supplier/explicit tick arguments.
2. Extract `LegacyPowerPathReadout` value record.
3. Extend the path adapter contract with per-cable rating, base-tile availability, fire action, and shared-lock sentinel operations.
4. Implement `LegacyPowerNodePath` while preserving exact state transitions and overflow behavior.
5. Add boundary tests for tick rollover, same-tick aggregation, voltage max semantics, decay windows, overload thresholds, null lock sentinel, cable-specific fire thresholds, and readout non-mutation.
6. Only then begin `PowerNodes` transfer extraction.

## Not implemented / not claimed

- No `PowerNodePath` implementation has been added in this audit commit.
- No voltage or amperage distribution has been ported.
- No real cable fire, lock, or server tick integration exists in the pure-Java prototype.
- No claim of runtime parity or successful game integration is made.
