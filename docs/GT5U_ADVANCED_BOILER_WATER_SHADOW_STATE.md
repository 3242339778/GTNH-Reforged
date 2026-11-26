# GT5U Advanced Boiler Water Shadow State

Status: pure Java compatibility primitive and tests; no NeoForge adapter.

## Purpose

Keep the exact signed water amount and signed steam-to-water conversion remainder used by GT5U while exposing only non-negative quantities to NeoForge. The state also reproduces the original water tank fill arithmetic and next-pre-tick cleanup condition.

## Source behavior preserved

- `MTEBoiler.produceSteam` subtracts steam from `mExcessWater`, computes integer water consumption using `GTValues.STEAM_PER_WATER`, subtracts that count directly from `mFluid.amount`, and does not clamp the result.
- `MTEBasicTank.onPreTick` clears the fillable stack on the server when fluid changing is allowed and its amount is <= 0.
- `MTEBasicTank.fill` calculates `capacity - existingAmount` without clamping a transient negative amount. A -1 amount therefore has capacity+1 available space.
- The conversion remainder is independent state and is not cleared by water-stack cleanup.

## Class and boundaries

`LegacyBoilerWaterShadowState` owns:
- nullable legacy fluid identity;
- signed legacy water amount;
- signed conversion remainder.

It provides exact conversion arithmetic, source-equivalent fill simulation/mutation, source-timed pre-tick cleanup, a non-negative API projection, and coherent snapshot/restore.

The adapter must never pass `legacyAmount()` directly into NeoForge. Use `exposedAmount()` and `exposedFluidId()`. A non-positive legacy stack projects as empty without normalizing the authoritative signed state.

This class models the tank-local fill semantics only. Machine-wide timer/data/input-disable gates, side predicates, Covers, NeoForge fluid identity/components, persistence encoding, dirty marking, and external transaction lifecycle remain adapter responsibilities.

## Transaction boundary

The snapshot captures identity, signed amount and signed remainder together. A transaction adapter can snapshot before the first mutation at a transaction depth, retain the snapshot until commit/abort, and restore it on rollback. Commit keeps the mutation. This class does not implement NeoForge Transaction and does not perform irreversible side effects.

A future boiler aggregate journal must combine this snapshot with steam inventory and every other mutable field an external transfer can change.

## Tests

Tests cover scarcity arithmetic (1 water and 375 steam -> -1 water and -55 remainder), non-negative projection, negative/zero cleanup timing, negative-window fill including capacity+1, simulation without mutation, empty-tank fill, fluid mismatch, atomic snapshot/restore, and committed state retention.
