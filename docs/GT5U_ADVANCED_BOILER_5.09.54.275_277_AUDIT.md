# MTEAdvancedBoilerBase Semantic Audit: GT5-Unofficial .275 → .277

**Source:** GT5-Unofficial  
**Compared versions:** 5.09.54.275 and 5.09.54.277  
**Target:** GTNH-Reforged legacy behavior preservation  
**Status:** Source-level review completed for the directly changed Advanced Boiler class; game runtime verification pending.

## 1. Fuel energy accounting

### .275
`consumeFuel()` directly increments `mProcessingEnergy`:

```java
this.mProcessingEnergy += burnTime / 10;
```

### .277
`consumeFuel()` now calls:

```java
addProcessingEnergy(burnTime / 10);
```

The inherited MTEBoiler helper also updates `fuelMaxEnergy`:

```java
protected void addProcessingEnergy(int amount) {
    this.mProcessingEnergy += amount;
    this.fuelMaxEnergy = Math.max(this.fuelMaxEnergy, this.mProcessingEnergy);
}
```

**Semantic difference:** fuel injection continues to add the same amount to processing energy, but now tracks the maximum stored energy for fuel-progress presentation. The previous direct field increment did not update `fuelMaxEnergy` through this code path.

## 2. Steam production and water-loss handling override

In .275, Advanced Boiler did not override `canNotCreateSteam()`; it inherited the MTEBoiler implementation. Its `onDangerousWaterLack()` override was empty, so the parent method's dangerous-water callback did not explode the Advanced Boiler.

In .277, Advanced Boiler explicitly overrides `canNotCreateSteam()`:

- Only executes its production-state logic on ticks divisible by 10.
- At temperature > 100 and with valid water present, produces `getProductionPerSecond() / 2` and sets `playBoiling = true`.
- At temperature <= 100, sets `playBoiling = false`.
- At temperature > 100 but without water, does not produce steam and does not explicitly update `mHadNoWater` or `playBoiling`.
- Always returns false, so this override does not interrupt the parent's post-tick sequence via the boolean return.

It also overrides `isProducingSteam()` to require temperature > 100 **and** valid water fluid with positive amount.

### Potential state quirk to preserve pending runtime verification

When temperature remains above 100 and water is absent, `playBoiling` is not explicitly cleared by this override. If previously true, it may remain true. This is a source-level control-flow observation, not yet a confirmed in-game outcome.

The new override also bypasses the inherited `canNotCreateSteam()` water-loss state updates, including the parent's `mHadNoWater` transitions. This should be documented as intentional compatibility behavior unless further evidence indicates otherwise.

## 3. Waila data

The .277 class adds Waila-related imports and overrides `getWailaBody()` and `getWailaNBTData()`.

- Body: calls the parent body builder, reads the Waila NBT boolean `isProducingSteam` and integer `power`, then appends the localized steam-production display when the boolean is true.
- NBT: calls the parent data provider, then writes `isProducingSteam`, `power`, and `fuel`.
- Fuel value: uses current `mProcessingEnergy` if positive; writes 0 if there is no valid fuel in slot 2; otherwise writes -1 (fuel is available but processing energy is not currently stored).

The parent boiler Waila presentation in .277 also distinguishes Advanced Boiler fuel progress scaling. UI behavior should be considered separately from fuel mechanics.

## 4. Unchanged fuel and heat mechanics within this class

The reviewed class retains the following source expressions:

- Burn time lookup: GameRegistry fuel value first, furnace fuel value fallback.
- Fuel eligibility: burn time > 0.
- Fuel injection eligibility: `mTemperature <= 101`.
- Processing energy contribution: `burnTime / 10` (now routed through the helper).
- Temperature contribution: `burnTime / 500`.
- Fuel slot decrement and randomized ash output remain in the reviewed method.
- Production per second: `750 * tier`.
- Steam production in the new override: half the per-second production value per qualifying 10-tick execution.

No claims are made about overall steam throughput until tick-based runtime testing accounts for the 10-tick cadence and other boiler state.

## 5. Compatibility conclusions

- Preserve the .277 helper-based energy bookkeeping, including `fuelMaxEnergy`.
- Preserve the .277 custom water-gated steam-production logic.
- Preserve the empty dangerous-water callback and newly bypassed inherited water-loss state behavior unless further evidence warrants a compatibility correction.
- Preserve the `playBoiling` high-temperature water-loss edge case pending verification.
- Treat Waila presentation/data as a separate presentation/network compatibility concern.
- Do not silently normalize or repair these states in the 26.3 implementation.

## 6. Verification status

**Source verified:** directly fetched class contents at refs 5.09.54.275 and 5.09.54.277 and compared the methods.

**Runtime verification pending:** no GTNH game-world test yet. The potential stale `playBoiling` state and Waila presentation should be verified in-game or with a behavioral harness before being elevated to confirmed runtime observations.
