# Tobi Mod — Kamui Intangibility: Complete Technical Reference

**Last Updated:** 2026-08-07  
**Target:** Minecraft Java Edition 1.21.1 · NeoForge 21.1.218 · Java 21  
**Mod ID / package:** `tobimod` · `com.tobi.tobimod`  
**Primary use:** Single-player. Multiplayer compatibility can be redesigned later.

---

# Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Dual-Mode Virtual Floor System — Complete Design](#2-dual-mode-virtual-floor-system--complete-design)
3. [Water As Solid — Design & Bug Fix](#3-water-as-solid--design--bug-fix)
4. [Network Protocol](#4-network-protocol)
5. [Client-Side Prediction](#5-client-side-prediction)
6. [Mixin Architecture](#6-mixin-architecture)
7. [Pose Suppression](#7-pose-suppression)
8. [Damage Protection System](#8-damage-protection-system)
9. [Complete File Map](#9-complete-file-map)
10. [Bug History & Root Causes](#10-bug-history--root-causes)
11. [Critical Things To Avoid](#11-critical-things-to-avoid)
12. [Underground Detection — How It Works](#12-underground-detection--how-it-works)
13. [Water Detection — How It Works](#13-water-detection--how-it-works)
14. [Conditional Packet Sync](#14-conditional-packet-sync)
15. [Known Gaps / Not Yet Implemented](#15-known-gaps--not-yet-implemented)
16. [Downgrade Notes (1.21.1 → other versions)](#16-downgrade-notes-1211--other-versions)
17. [Debugging Tips](#17-debugging-tips)

---

# 1. Architecture Overview

## Design Philosophy

Obito walks through walls with **vanilla physics** — no floating, no flight, no hover. He stands on the ground, walks into a wall, and phases through it. Gravity still applies. He sinks through floors by choice (shift) and rises by jumping (underground smooth movement). **No creative flight. No permanent `setNoGravity(true)`.**

## The Two Modes

The system runs in one of two modes at any time, automatically switching:

```
┌─────────────────┐         body enters solid/water         ┌──────────────────┐
│   SURFACE MODE  │ ──────────────────────────────────────► │ UNDERGROUND MODE │
│                 │                                         │                  │
│ • Vanilla jump  │         body leaves solid/water         │ • Smooth 0.15/tick│
│ • Step-Up +1    │ ◄────────────────────────────────────── │ • Gravity OFF    │
│ • Shift-sink    │                                         │ • Vanilla jump   │
│ • FloorY clamp  │                                         │   suppressed     │
│ • Gravity ON    │                                         │ • Water climbable│
└─────────────────┘                                         └──────────────────┘
```

Mode is determined by `isBodyInsideSolid(player)` which checks block at body level (Y+1). **Water source blocks count as solid** for this check (see §3).

## Core Mechanism — Every Tick

```
1. noPhysics = true          → player passes through ALL blocks
2. Gravity: surface=ON, underground=OFF
3. No flight                 → no creative flight, no flying speed boost
4. Virtual floor at floorY   → clamp Y ≥ floorY when falling
5. resetFallDistance()        → no fall damage ever
```

## Duration / Cooldown

```
Maximum duration:            60 seconds (1200 ticks)
Natural expiry cooldown:     5 seconds (100 ticks)
Manual toggle-off cooldown:  1 second (20 ticks)
Attack vulnerability:        3 ticks (0.15 seconds)
```

---

# 2. Dual-Mode Virtual Floor System — Complete Design

## 2.1 Underground Mode

Triggered when: `isBodyInsideSolid(player)` returns true (solid collision OR water source at body level Y+1).

### Movement

| Input | Effect | Speed |
|-------|--------|-------|
| Hold jump | Smooth upward | +0.15 blocks/tick (direct position) |
| Hold shift | Smooth downward | -0.15 blocks/tick (direct position) |
| Look down 45°+ + hold shift | Quick Descend | -0.30 blocks/tick (2x speed) |
| Release jump | Round Y up | `Math.ceil(Y)`, if exactly integer go +1 |
| Release shift | Round Y down | `Math.floor(Y)` |
| Look up 45°+ + tap jump | Phase Ascend | Instant teleport to surface |

### Physics overrides

```java
player.setNoGravity(true);                    // no gravity pull
player.setDeltaMovement(xVel, 0.0, zVel);     // zero Y velocity every tick
player.setOnGround(false);                    // suppress vanilla jumpFromGround
```

### How smooth movement works

The **client** detects jump/shift via GLFW and sends `KamuiVerticalMovePayload(jumpHeld)` to the server. Both client and server then apply `player.setPos(x, Y ± 0.15, z)` every tick and update `floorY` to match.

The jump/shift keys are read via **GLFW directly** (`glfwGetKey()`) instead of Minecraft's `KeyMapping.isDown()` because Minecraft resets `isDown` after manipulating the player entity, causing missed inputs.

### Phase Ascend

When looking up 45°+ and tapping jump while inside a solid block:
1. Scan upward from current Y for first air block with solid/water-source below
2. Teleport player to that Y
3. Set floorY to surface Y
4. Zero Y velocity, set onGround
5. Force sync state to client

### Water surface behavior

When in underground mode inside water:
- Hold jump → rise through water at 0.15/tick
- When body exits water top → auto-switch to surface mode
- Surface mode snaps floorY to water surface (waterSourceY + 1.0)
- Player stands on water surface like a solid block

## 2.2 Surface Mode

Triggered when: `isBodyInsideSolid(player)` returns false (body at Y+1 is in air).

### Movement

| Input | Effect |
|-------|--------|
| Jump (vanilla) | Normal vanilla jump arc |
| Hold jump (Step-Up) | +1 floorY every 4 ticks, with solid-below check |
| Hold shift (sink) | Instant -1 on first press, then -1 every 5 ticks |
| Walk into wall | Body enters solid → auto-switch to underground |

### Jitter Prevention

Gravity is applied inside `travel()`, which runs between Pre-tick and Post-tick. Without intervention, the player falls ~0.003 blocks per tick below floorY and gets clamped back, causing visible screen flicker (Y flickers 63.99999 → 64.0 → 63.99999).

**Fix (Pre-tick):** When player is at floorY and not rising:
1. Snap Y to exactly floorY
2. Zero Y velocity
3. Set `onGround = true`
4. Set `noGravity = true` (prevents travel() from applying the -0.08 gravity pull)

**Fix (Post-tick):** Restore `noGravity = false` so gravity works for jumps and falls.

### Underground → Surface Transition

When transitioning from underground to surface mode:
1. `findSupportBelow()` scans for solid/water below current Y
2. `getWaterSurfaceY()` checks for water surface above
3. floorY = max(supportBelow, waterSurface)
4. If player Y < floorY, snap player up to floorY
5. Set onGround = true

### Auto-Adjust

If the block below floorY has no solid collision AND is not a water source:
1. `findSupportBelow()` scans down for next support
2. If found support < current floorY, drop floorY to it

This handles walking over gaps and cliff edges. **Water source blocks are treated as support** so auto-adjust won't drop floorY through water.

### Shift-Sink

Only active when player is on the virtual floor (`|Y - floorY| < ε`):
- **First press:** Instant -1 floorY, snap player, find support below
- **Holding:** Accumulate ticks, every 5 ticks: -1 floorY, snap, find support

### Floor Chase

When shift is NOT held and player Y > floorY + 0.5:
- If feet are inside solid/water: raise floorY to player Y
- This catches the case where the player walks horizontally into a wall while above floorY

### Virtual Floor Enforcement

```java
if (player.getY() < floorY && yVel <= 0.0) {
    player.setPos(x, floorY, z);
    player.setDeltaMovement(xVel, 0.0, zVel);
    player.setOnGround(true);
} else if (|player.getY() - floorY| < ε && yVel <= 0.0) {
    player.setOnGround(true);  // landing on floor
}
```

## 2.3 Constants

```java
SMOOTH_SPEED        = 0.15    // underground movement speed (blocks/tick)
SINK_TICKS_PER_BLOCK = 5      // surface shift-sink rate
STEP_UP_TICKS       = 4      // surface step-up interval
FLOOR_EPSILON       = 0.05   // floor snap tolerance
JUMP_RAISE_COOLDOWN = 4      // ticks between step-up raises
LOOK_UP_PITCH       = -45.0  // xRot threshold for Phase Ascend / Quick Descend
MAX_SUPPORT_SCAN    = 128    // max depth for findSupportBelow
```

---

# 3. Water As Solid — Design & Bug Fix

## The Problem

Minecraft water source blocks have **empty collision shapes**. `BlockState.getCollisionShape()` returns an empty VoxelShape for water. This means every check that used `getCollisionShape()` to determine "is this block solid?" was completely blind to water.

## The Original Bug (reported by user)

> "If I'm at Y63 and my lower body is half-submerged in water, then I activate my ability and hold spacebar to go up. It will go up but it won't elevate the new flooring, making me fall down every time I try to go up to Y64."

**Root cause chain:**

1. Player at Y63 in water → body at Y64 is water source → `isBodyInsideSolid` correctly returns true → **underground mode** ✅
2. Hold jump → Y goes up smoothly, `state.floorY(newY)` tracks it ✅
3. Player reaches Y64 → body at Y65 is air → **switches to surface mode** ✅
4. **Surface auto-adjust runs:** checks block below floorY with `hasSolidCollision()` ❌
5. Water source has **empty collision shape** → `hasSolidCollision()` returns false ❌
6. `findSupportBelow()` also used raw `getCollisionShape()` → **skips water** → finds nothing ❌
7. Auto-adjust drops floorY back down → player falls back into water ❌❌❌

## The Fix — `hasSolidCollision()` Enhancement

### Server (KamuiIntangibilityHandler.java)

```java
/** Check if block has solid collision shape OR is a water source block. */
private static boolean hasSolidCollision(Level level, BlockPos pos) {
    BlockState blockState = level.getBlockState(pos);
    if (!blockState.getCollisionShape(level, pos).isEmpty()) return true;
    // Water source blocks count as solid for Kamui
    FluidState fluidState = level.getFluidState(pos);
    return fluidState.is(FluidTags.WATER) && fluidState.isSource();
}
```

### Client (ClientEventHandler.java)

```java
/** Check if block has solid collision OR is a water source (for Kamui water-as-solid). */
private static boolean hasSolidOrWaterCollisionClient(Player player, BlockPos pos) {
    BlockState state = player.level().getBlockState(pos);
    if (!state.getCollisionShape(player.level(), pos).isEmpty()) return true;
    FluidState fluidState = player.level().getFluidState(pos);
    return fluidState.is(FluidTags.WATER) && fluidState.isSource();
}
```

## What Uses `hasSolidCollision` / `hasSolidOrWaterCollisionClient`

Every single one of these must use the water-aware version:

| Function | Purpose | Must include water? |
|----------|---------|---------------------|
| `isBodyInsideSolid()` | Underground mode detection | **YES** |
| `isFeetInsideSolid()` | Phase Ascend trigger | **YES** |
| `findSupportBelow()` | Find floor below player | **YES** — water is support |
| `findSurfaceAbove()` | Phase Ascend target | **YES** — water surface is a surface |
| Auto-adjust check | Drop floor if no support | **YES** — don't drop through water |
| Floor chase | Raise floor when in solid | **YES** — chase through water |
| Step-Up solid-below check | Validate +1 raise | **YES** — can step up onto water |

## Water Surface Detection — `getWaterSurfaceY()`

```java
private static double getWaterSurfaceY(ServerPlayer player, double fromY) {
    // Scan up from fromY to find topmost water source block
    // Returns topWaterY + 1.0 (standing Y on water surface)
    // Returns fromY if no water found
}
```

Used in:
- **Underground → surface transition:** Snap floorY to water surface if player just exited water
- **Surface mode water snap:** If player Y is within 1 block of a water surface and water surface > current floorY, raise floorY to water surface

## Water Behavior Summary

| Scenario | Behavior |
|----------|----------|
| Body in water source (Y+1) | Underground mode — smooth swim up/down |
| Reach water surface top | Auto-transition to surface mode, floorY = waterSurfaceY + 1.0 |
| Walk on water surface | Surface mode, floorY clamped to water top |
| Walk from land into water at same Y | Smooth transition, water treated as solid floor |
| Shift-sink in water | Sinks through water, findSupportBelow finds bottom |
| Phase Ascend through water | Scans up treating water as solid, exits at air above water |
| Standing on water, no keys held | Stays on water surface (floorY = water top) |

---

# 4. Network Protocol

## Packets

### Server → Client

**`KamuiIntangibilityStatePayload`**

```
Fields:  boolean active, double floorY, int remainingSeconds, boolean underground
Purpose: Syncs Kamui state from server to client for prediction
Sent:    On activation, deactivation, Phase Ascend, and when floorY changes
```

Client stores these in static fields:
- `clientKamuiActive`
- `clientFloorY` — used for client prediction
- `clientUnderground` — used to select underground vs surface handler
- `clientRemainingSeconds` — for HUD timer

### Client → Server

**`KamuiIntangibilityTogglePayload`**

```
Fields:  (none)
Purpose: Toggle Kamui on/off
Sent:    When R keybind pressed
```

**`KamuiJumpPayload`**

```
Fields:  boolean stepUp
Purpose: Phase Ascend (stepUp=false) or Step-Up (stepUp=true)
Sent:    On jump input during Kamui
```

**`KamuiVerticalMovePayload`**

```
Fields:  boolean jumpHeld
Purpose: Underground jump press/release
Sent:    When jump state changes while in underground mode
```

## Why Custom Packets Exist

- **`player.jumping`** is `protected` on `LivingEntity` — server can't access it
- Server-side `jumping` field is never set by the client automatically
- Minecraft's `KeyMapping.isDown()` resets after player manipulation
- Solution: Client reads GLFW directly + sends custom packets

## Packet Registration (PacketHandler.java)

```java
registrar.playToServer(KamuiIntangibilityTogglePayload.TYPE, ...);
registrar.playToServer(KamuiJumpPayload.TYPE, ...);
registrar.playToServer(KamuiVerticalMovePayload.TYPE, ...);
registrar.playToClient(KamuiIntangibilityStatePayload.TYPE, ...);
```

---

# 5. Client-Side Prediction

The client mirrors the server's logic to keep movement smooth between server ticks:

### Underground Mode (client)

```java
if (undergroundJumpHeld) {
    double newY = player.getY() + SMOOTH_SPEED;    // 0.15
    player.setPos(player.getX(), newY, player.getZ());
    KamuiIntangibilityStatePayload.predictFloorSet(newY);
}
```

### Surface Mode (client)

- Jitter prevention (snap to floorY, zero Y vel, noGravity)
- Step-Up prediction (`predictFloorRaise()`)
- Phase Ascend prediction (`predictFloorSet(surfaceY)`)
- Safety-net floor clamp in Post-tick

### Prediction Methods

```java
KamuiIntangibilityStatePayload.predictFloorRaise()   // floorY += 1.0
KamuiIntangibilityStatePayload.predictFloorSet(y)    // floorY = y
```

These update the static `clientFloorY` immediately so the client doesn't have to wait for the server round-trip. When the server's state packet arrives, it overwrites with the authoritative value.

---

# 6. Mixin Architecture

## LivingEntityKamuiMixin

```java
@Mixin(LivingEntity.class)
@Inject(method = "tickEffects", at = @At("TAIL"))
private void tobimod$applyKamuiPhysics(CallbackInfo ci)
```

- Sets `noPhysics = true` and `resetFallDistance()` every tick while Kamui active
- Runs on both client and server (reads different state sources)
- Server: reads `KamuiIntangibilityState` Attachment
- Client: reads `KamuiIntangibilityStatePayload` static fields

## EntityKamuiMixin

```java
@Mixin(Entity.class)
@Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
private void tobimod$preventSuffocation(CallbackInfoReturnable<Boolean> cir)
```

- Returns `false` while Kamui active → prevents suffocation damage inside blocks
- Same client/server split as LivingEntityKamuiMixin

## Mixin Registration

File: `src/main/resources/tobimod.mixins.json`

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.tobi.tobimod.mixin",
  "compatibilityLevel": "JAVA_21",
  "refmap": "tobimod.refmap.json",
  "mixins": [
    "LivingEntityKamuiMixin",
    "EntityKamuiMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

In `META-INF/neoforge.mods.toml`:
```toml
[[mixins]]
config="tobimod.mixins.json"
```

---

# 7. Pose Suppression

While Kamui is active, force `Pose.STANDING` to prevent:

- Swimming animation when in water
- Crawling animation when in 1-block gaps
- Elytra fall-flying animation
- Sleeping pose

Checked every Post-tick:
```java
Pose currentPose = player.getPose();
if (currentPose == Pose.SWIMMING || currentPose == Pose.CROUCHING ||
        currentPose == Pose.FALL_FLYING || currentPose == Pose.SLEEPING) {
    player.setPose(Pose.STANDING);
}
```

Also prevents vehicle re-mount: if `player.isPassenger()`, calls `player.stopRiding()` every tick.

---

# 8. Damage Protection System

## Protected State

```
Kamui active AND not inside the 3-tick attack vulnerability window
```

## Events Used

| Event | Purpose |
|-------|---------|
| `EntityInvulnerabilityCheckEvent` | Early damage immunity |
| `ProjectileImpactEvent` | Cancel + discard projectiles |
| `AttackEntityEvent` | Trigger 3-tick vulnerability on attack |
| `LivingKnockBackEvent` | Cancel combat knockback |
| `ExplosionEvent.Detonate` | Remove player from explosion affected entities |

## Attack Vulnerability

When the player attacks while Kamui is active:
```java
state.makeVulnerable(gameTime);
// → vulnerabilityEndsAt = gameTime + 3 ticks
```

During those 3 ticks, `isProtected()` returns false and the player can take damage.

---

# 9. Complete File Map

## Changed files (this session — water-as-solid fix)

| File | Changes |
|------|---------|
| `KamuiIntangibilityHandler.java` | `hasSolidCollision()` now includes water sources; `findSupportBelow()` uses `hasSolidCollision()`; `findSurfaceAbove()` uses `hasSolidCollision()`; added `isWaterSource()`, `getWaterSurfaceY()`; water surface snap in surface mode; underground→surface transition checks water surface |
| `ClientEventHandler.java` | Added `hasSolidOrWaterCollisionClient()`; all client helpers use it; added `FluidTags`/`FluidState` imports |

## All Kamui-related files

```
src/main/java/com/tobi/tobimod/
├── common/abilities/
│   ├── KamuiIntangibilityHandler.java    # Server: dual-mode logic, water support, pose suppression
│   ├── KamuiIntangibilityState.java      # Attachment: floorY, sinkAccumulator, jumpEscapeConsumed
│   └── KamuiChannelHandler.java          # 3-second channel for absorption (unchanged)
├── client/
│   └── ClientEventHandler.java           # Client: GLFW keys, dual-mode, water support, prediction
├── mixin/
│   ├── LivingEntityKamuiMixin.java       # noPhysics + resetFallDistance every tick
│   └── EntityKamuiMixin.java             # Prevents isInWall suffocation
├── network/
│   ├── PacketHandler.java                # Registers all Kamui payloads
│   └── payload/
│       ├── KamuiIntangibilityTogglePayload.java  # Toggle on/off
│       ├── KamuiIntangibilityStatePayload.java   # Server→Client state (floorY, underground)
│       ├── KamuiJumpPayload.java                 # Phase Ascend / Step-Up
│       └── KamuiVerticalMovePayload.java         # Underground jump held/released
└── TobiMod.java                                  # KAMUI_INTANGIBILITY_STATE attachment registration
```

---

# 10. Bug History & Root Causes

These are ALL bugs encountered across the entire development history. **Every single one was fixed.** This list exists so future AI sessions don't reintroduce them.

## Bug 1: Jitter (63.99999 flicker)

**Symptom:** Screen flickers rapidly when standing on virtual floor.  
**Root cause:** Gravity applied in `travel()` between Pre-tick and Post-tick. Player falls ~0.003 blocks below floorY, gets clamped back.  
**Fix:** Pre-tick gravity suppression — snap Y to floorY, zero Y vel, temporarily set `noGravity = true`. Post-tick restores `noGravity = false`.

## Bug 2: Jump not raising floorY

**Symptom:** Pressing jump does nothing to floorY.  
**Root cause:** `player.jumping` is `protected` on `LivingEntity` — server code can't access it. Server-side `jumping` field never gets set by the client.  
**Fix:** Client detects jump via GLFW key detection + sends `KamuiJumpPayload` to server.

## Bug 3: Auto-adjust undoing jump-raises

**Symptom:** Jump raises floorY +1 but auto-adjust immediately drops it back.  
**Root cause:** `findSupportBelow` ran unconditionally every tick, found no solid below the raised floorY, and reset it.  
**Fix:** Replaced with dual-mode system where auto-adjust only runs in surface mode, and water source blocks count as support.

## Bug 4: `jumping` field always false on server

**Symptom:** Server never receives jump input from client.  
**Root cause:** Vanilla Minecraft doesn't sync the `jumping` field to the server.  
**Fix:** Client detects jump via GLFW `glfwGetKey()` and sends `KamuiVerticalMovePayload`.

## Bug 5: `KeyMapping.isDown()` resetting

**Symptom:** Jump detection works for one tick then stops.  
**Root cause:** Minecraft's key binding system resets `isDown` after player manipulation in the same tick.  
**Fix:** Read GLFW directly via `isJumpKeyPhysicallyHeld()` / `isShiftKeyPhysicallyHeld()`.

## Bug 6: Floor chase fighting shift-sink

**Symptom:** Holding shift to sink through floor doesn't work; floorY bounces back up.  
**Root cause:** Floor chase raised floorY back up after shift lowered it.  
**Fix:** Disable floor chase while shift is held.

## Bug 7: Hold jump only raising once

**Symptom:** Holding jump in surface mode raises floorY by 1, then stops.  
**Root cause:** `onFloor` check prevented Step-Up while player was mid-jump arc above the floor.  
**Fix:** Remove `onFloor` requirement for Step-Up; use tick counter instead.

## Bug 8: Elevation bug (tap=+1, hold=+2)

**Symptom:** Tapping jump raises +1, but holding raises +2 per cycle.  
**Root cause:** Vanilla jump fired alongside Step-Up, causing double raise.  
**Fix:** Underground mode completely suppresses vanilla jump (`setOnGround(false)`). Surface mode uses `suppressVanillaJump` flag.

## Bug 9: Can't stand on water surface (THIS SESSION)

**Symptom:** Starting in water at Y63, holding jump goes up but floorY doesn't elevate to water surface at Y64. Player falls back down every time.  
**Root cause:** Water blocks have **empty collision shapes**. `hasSolidCollision()` only checked `getCollisionShape()`, returned false for water. `findSupportBelow()` also skipped water. Auto-adjust dropped floorY through water.  
**Fix:** 
- `hasSolidCollision()` now returns true for water source blocks
- `findSupportBelow()` uses `hasSolidCollision()` instead of raw shape check
- `findSurfaceAbove()` uses `hasSolidCollision()`
- Added `getWaterSurfaceY()` for water surface detection
- Surface mode water snap raises floorY to water surface
- Underground→surface transition checks water surface
- Client `hasSolidOrWaterCollisionClient()` mirrors server logic

---

# 11. Critical Things To Avoid

## ❌ NEVER use `getCollisionShape().isEmpty()` alone to check if a block is "solid"

Water source blocks have empty collision shapes. If you need to know "can the player stand on this?" or "is this block solid for Kamui purposes?", you **MUST** use `hasSolidCollision()` (server) or `hasSolidOrWaterCollisionClient()` (client). These include water source detection.

**Wrong:**
```java
BlockState bs = level.getBlockState(pos);
if (!bs.getCollisionShape(level, pos).isEmpty()) { ... }  // ❌ MISSES WATER
```

**Right:**
```java
if (hasSolidCollision(level, pos)) { ... }  // ✅ Includes water sources
```

## ❌ NEVER use `player.jumping` on the server

It's `protected` on `LivingEntity` and the client never syncs it. Use client-side GLFW detection + custom packets.

## ❌ NEVER use `KeyMapping.isDown()` for Kamui input

Minecraft resets `isDown` after manipulating the player. Use GLFW directly:
```java
GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_TRUE
```

## ❌ NEVER set `setNoGravity(true)` permanently

Only use it temporarily in Pre-tick for jitter prevention, then restore in Post-tick. Underground mode can disable gravity, but surface mode MUST have gravity enabled for vanilla jump to work.

## ❌ NEVER enable creative flight

No `CREATIVE_FLIGHT` attribute modifier. No `player.getAbilities().flying = true`. The entire design philosophy is vanilla physics + noPhysics.

## ❌ NEVER scan for solid blocks without including water

Every helper that checks "is this block passable?" must account for water sources:
- `findSupportBelow()` — must use `hasSolidCollision()`
- `findSurfaceAbove()` — must use `hasSolidCollision()`
- `isBodyInsideSolid()` — must use `hasSolidCollision()`
- `isFeetInsideSolid()` — must use `hasSolidCollision()`
- Step-Up solid-below check — must use `hasSolidCollision()`
- Floor chase check — must use `hasSolidCollision()`
- Auto-adjust check — must use `hasSolidCollision()`

## ❌ NEVER run floor chase while shift is held

Floor chase raises floorY when the player is inside a solid above the floor. If shift is sinking the player down, floor chase will fight it and bounce floorY back up.

## ❌ NEVER run auto-adjust unconditionally

Auto-adjust drops floorY when no support is found below. It must only run when the player is NOT transitioning from underground mode and NOT in the middle of a jump/step-up. The current implementation handles this by only running in surface mode and checking `hasSolidCollision` (which includes water).

## ❌ NEVER send state packets every tick

Use `syncStateIfNeeded()` which only sends when `abs(currentFloor - lastSynced) > 0.01`. Use `forceSyncState()` only for activation, deactivation, and Phase Ascend.

## ❌ NEVER use `setHealth(getMaxHealth())` every tick

This makes the player constantly heal. Apply health once on activation, not repeatedly.

## ❌ NEVER rely on `isInWall()` returning true while Kamui is active

The `EntityKamuiMixin` forces `isInWall()` to return false. If you need to know if the player is inside a block, use `isBodyInsideSolid()` or `isFeetInsideSolid()`.

## ❌ NEVER store long-lived entity references in state

Store UUIDs, not Entity objects. Entity references become stale after death/despawn/dimension change.

## ❌ NEVER forget to clean up per-player tracking maps on deactivation

All maps (`LAST_JUMP_RAISE_TICK`, `PREV_SHIFT_DOWN`, `UNDERGROUND_JUMP_HELD`, `LAST_SYNCED_FLOOR_Y`, `PREV_UNDERGROUND`) must be cleared in `deactivate()` and when Kamui expires or player logs out.

---

# 12. Underground Detection — How It Works

## `isBodyInsideSolid(ServerPlayer player)`

Checks the block at **body level** (Y + 1.0):

```java
BlockPos bodyPos = BlockPos.containing(player.getX(), player.getY() + 1.0D, player.getZ());
return hasSolidCollision(level, bodyPos);
```

Why body level (Y+1)?
- Player hitbox is ~1.8 blocks tall
- Feet at Y, head at Y+1.8
- Body/waist at Y+1 is the "am I inside something?" check
- Tall grass at feet level has no collision shape → correctly ignored
- Water source at body level → detected as underground (can swim up/down)

## `isFeetInsideSolid(ServerPlayer player)`

Checks block at **feet level** (Y):

```java
BlockPos feetPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
return hasSolidCollision(level, feetPos);
```

Used for Phase Ascend trigger check — player must be inside a block to ascend out of it.

## `BlockPos.containing()` vs `new BlockPos(Mth.floor(), ...)`

Both work for integer conversion. `containing()` is more concise. `Mth.floor()` is used in scan loops for clarity.

---

# 13. Water Detection — How It Works

## `isWaterSource(Level level, BlockPos pos)`

```java
FluidState fluidState = level.getFluidState(pos);
return fluidState.is(FluidTags.WATER) && fluidState.isSource();
```

Key points:
- Must check `isSource()` — flowing water is NOT treated as solid (it has level < 15)
- `FluidTags.WATER` includes both regular water and flowing water variants
- `getFluidState()` can return a non-empty fluid state even for blocks that appear as air to collision shapes
- **Import required:** `net.minecraft.tags.FluidTags` and `net.minecraft.world.level.material.FluidState`

## `getWaterSurfaceY(ServerPlayer player, double fromY)`

Scans upward from `fromY` to find the topmost contiguous water source block:

```java
int topWaterY = -999;
for (int y = startY; y < startY + 20; y++) {
    if (isWaterSource(level, pos)) {
        topWaterY = y;
    } else {
        break;  // hit non-water, stop
    }
}
return topWaterY + 1.0D;  // standing Y on top of water
```

The +1.0 is because a water source at block Y occupies Y to Y+1, and the player stands on top at Y+1.

## Why only source blocks?

- Source blocks are stable (level = 15, full block)
- Flowing water has variable height and direction — player would slide
- Standing on flowing water would be inconsistent and confusing
- Rivers in Minecraft are mostly source blocks, so this covers the common case

---

# 14. Conditional Packet Sync

## `syncStateIfNeeded()`

```java
private static void syncStateIfNeeded(ServerPlayer player, KamuiIntangibilityState state, boolean underground) {
    double currentFloor = state.floorY();
    double lastSynced = LAST_SYNCED_FLOOR_Y.getOrDefault(player.getUUID(), Double.NaN);
    if (Double.isNaN(lastSynced) || Math.abs(currentFloor - lastSynced) > 0.01D) {
        forceSyncState(player, state);
    }
}
```

Only sends a state packet when floorY has changed by more than 0.01 blocks since last sync. Called at the end of every Post-tick.

## `forceSyncState()`

Always sends, updates `LAST_SYNCED_FLOOR_Y`. Used for:
- Activation
- Deactivation
- Phase Ascend
- Jump release rounding (vertical move payload handler)

## Tracking Map

```java
private static final Map<UUID, Double> LAST_SYNCED_FLOOR_Y = new HashMap<>();
```

Must5 Must be cleaned up on deactivation, logout, and expiry.

---

# 15. Known Gaps / Not Yet Implemented

| Feature | Status |
|---------|--------|
| Custom Kamui dodge sound | Not implemented — use built-in placeholder |
| Kamui Absorption / Release | Designed, not implemented |
| Self Warp round-trip | Designed, not implemented |
| Kamui Navigation GUI (C key) | Prototype screens exist, not functional |
| Waypoints | Not implemented |
| Manual coordinate teleport | Not implemented |
| Kamui Travel (G key) | Planned, low priority |
| Basic Genjutsu (V key) | Planned |
| Advanced Genjutsu (B key) | Planned |
| Quick Descend (look down 45°+ + shift) | **Implemented in design but NOT yet tested by user** |
| Water climbing (underground mode in water) | **Implemented but NOT yet tested by user** |
| Water surface standing | **Implemented but NOT yet tested by user** |
| Flowing water (non-source) behavior | Not handled — player falls through flowing water |

---

# 16. Downgrade Notes (1.21.1 → other versions)

## If downgrading to 1.20.x (Forge or NeoForge)

### Attachment System
- NeoForge 1.21.1 uses `DataAttachment` / `player.getData()` / `player.getExistingDataOrNull()`
- 1.20.x uses `Capability` or `AttachCapabilitiesEvent`
- **Must replace all Attachment code** with capability system

### Network Packets
- 1.21.1 uses `CustomPacketPayload` + `StreamCodec` + `Payload8tor.playToServer/playToClient`
- 1.20.x) uses `SimpleChannel` + `registerMessage()` with hand-written encode/decode
- **Must rewrite all payloads**

### Fluid Tags
- `FluidTags.WATER` exists in both 1.20.x and 1.21.1 — should work
- `FluidState.isSource()` → check if it's `isSource()` or `getAmount() == 8` in older versions

### Mixins
- Mixin format is mostly compatible
- `@Inject` at `tickEffects TAIL` may not exist in older versions — use `tick` instead
- Check `compatibilityLevel` in mixins.json

### BlockPos.containing()
- May not exist in older versions — use `new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z))`

### PlayerTickEvent
- NeoForge 1.21.1 uses `PlayerTickEvent.Pre` / `PlayerTickEvent.Post`
- Older versions may use `TickEvent.PlayerTickEvent` with `Phase.START` / `Phase.END`

### GLFW Key Detection
- `InputConstants.Key.getType()` / `getValue()` — API may differ slightly
- Core GLFW calls (`glfwGetKey`) are the same

## If downgrading to Fabric

- Replace all `@SubscribeEvent` with Fabric event callbacks
- Replace `DataAttachment` with Fabric `TrackedDataHandler` or custom cardinals
- Replace NeoForge network with Fabric networking API
- Replace `PlayerTickEvent` with Fabric tick events
- Mixins are largely the same (both use SpongePowered mixin)

---

# 17. Debugging Tips

## Common debugging scenarios

### Player falls through floor when standing still
- Check: Is `hasSolidCollision()` including water sources? (if near water)
- Check: Is auto-adjust dropping floorY incorrectly?
- Check: Is `findSupportBelow()` using `hasSolidCollision()` or raw `getCollisionShape()`?

### Jitter / screen flicker at floorY
- Check: Is Pre-tick gravity suppression working? (snap Y, zero vel, noGravity=true)
- Check: Is Post-tick restoring noGravity=false? (only in surface mode)

### Jump does nothing
- Check: Is `KamuiJumpPayload` being sent? (client GLFW detection)
- Check: Is `UNDERGROUND_JUMP_HELD` being set? (vertical move packet)
- Check: Is `isBodyInsideSolid()` returning true? (underground mode required for smooth movement)

### Can't stand on water
- Check: Does `hasSolidCollision()` include water source check?
- Check: Does `findSupportBelow()` use `hasSolidCollision()`?
- Check: Is `getWaterSurfaceY()` being called in surface mode?
- Check: Is auto-adjust dropping floorY through water? (if so, `hasSolidCollision` is broken)

### Phase Ascend doesn't work through water
- Check: Does `findSurfaceAbove()` use `hasSolidCollision()`?
- Check: Does `isFeetInsideSolid()` include water?

### Swimming animation while in water
- Check: Is pose suppression running? (check every Post-tick)
- Check: Is `player.setPose(Pose.STANDING)` being called?

### Player gets stuck in wall after Kamui deactivates
- This is expected behavior — vanilla suffocation kicks in
- The `EntityKamuiMixin` only suppresses `isInWall` while Kamui is active

## Logging

Add temporary logging to trace issues:
```java
// In onPlayerTickPost:
LOGGER.debug("Y={} floorY={} underground={} floorY-actual={}", 
    player.getY(), state.floorY(), underground, 
    hasSolidCollision(level, belowFloorPos));
```

Set log level in `run/config/log4j2.xml`:
```xml
<Logger level="debug" name="KamuiDebug" />
```

---

# Appendix A: Key State Flow (Full Tick Cycle)

```
════════════ PRE-TICK ════════════
  noPhysics = true
  if underground:
   * noGravity = true
    * zero Y velocity
    * onGround = false
  if surface:
    * if on floor + falling → snap Y, zero vel, onGround, noGravity=true

════════════ travel() ════════════
  (vanilla physics runs here — gravity pull, jump impulse, movement)
  noPhysics=true → skip block collision
  If noGravity=true (set in Pre-tick) → skip gravity pull

════════════ POST-TICK ════════════
  noPhysics = true
  if underground:
    * noGravity = true
    * if jumpHeld → Y += 0.15, floorY = Y
    * if shiftHeld → Y -= 0.15, floorY = Y
    * pose suppression
  if surface:
    * noGravity = false (restore for vanilla jump)
    * if wasUnderground → transition snap
    * water surface snap
    * auto-adjust floorY
    * shift-sink logic
    * floor chase
    * enforce virtual floor (clamp Y ≥ floorY)
    * pose suppression
  syncStateIfNeeded()
```

---

# Appendix B: Complete State Attachment Schema

```java
public final class KamuiIntangibilityState {
    // Lifecycle
    boolean active;
    long activeEndsAt;          // game tick when 60s expires
    long vulnerabilityEndsAt;   // game tick when attack vulnerability ends
    long cooldownEndsAt;        // game tick when next activation is allowed

    // Virtual Floor
    double floorY;              // Y level of the virtual floor
    int sinkAccumulator;        // ticks accumulated for continuous sinking
    boolean jumpEscapeConsumed; // prevents 20 raises/sec while holding jump

    // Saved originals (for restore on deactivation)
    boolean originalNoPhysics;
    boolean originalNoGravity;
    boolean originalFlying;
    float originalFlyingSpeed;
}
```

Codec: `RecordCodecBuilder` with 11 fields. Registered in `TobiMod.java` as a `DataAttachment`.

---

# Appendix C: What Was Removed (Do Not Re-Add)

These were intentionally removed and replaced by the dual-mode system:

```
❌ Surface / Underground two-mode system (old bool-based)
❌ isHeadInsideSolidBlock / hasValidSurfaceSpace / updateMovementMode / surfaceClearTicks
❌ tryEnterWall() / sinkIntoFloor()
❌ setNoGravity(true) permanently / creative flight / CREATIVE_FLIGHT attribute modifier
❌ Underground flying speed constant
❌ PRE_TRAVEL_Y_VEL map (velocity-based jump detection)
❌ JUMP_RAISE_IMMUNITY map (timer-based immunity)
❌ PlayerKamuiMixin / LivingEntityAccessor (deleted, not needed)
❌ Velocity-based jump detection (replaced by GLFW + packets)
❌ underground bool in state/payload (replaced by server-computed isBodyInsideSolid())
```

If any AI session suggests re-adding these, **do not**. They were removed for specific bug fixes documented in §10.