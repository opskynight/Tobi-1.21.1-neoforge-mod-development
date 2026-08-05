# Tobi Mod — Updated Master Plan (V2)

**Target:** Minecraft Java Edition 1.21.1 · NeoForge 21.1.218 · Java 21  
**Mod ID / package:** `tobimod` · `com.tobi.tobimod`  
**Primary use:** Single-player. Multiplayer compatibility can be redesigned later.  
**Scope:** Original Tobi/Obito ability set; no extra canon ability expansion unless explicitly decided later.

---

## Document Purpose

This replaces earlier planning documents as the current reference for future chats and implementation work. It contains:

- final/recent decisions for Combat Kamui Intangibility;
- its current technical implementation status;
- refined Kamui Absorption, Kamui Dimension, self-warp, waypoint, and GUI concepts;
- original remaining ability plans;
- performance and architecture rules;
- decisions still requiring confirmation.

When an older document conflicts with this one, **this document wins**.

---

# 1. Core Development Rules

1. **Server authoritative:** Client keybinds and GUI buttons request actions; the server validates cooldowns, targets, range, damage, dimensions, and teleports.
2. **Use ticks:** Minecraft runs at 20 ticks/second.
   - 0.15 seconds = 3 ticks
   - 1 second = 20 ticks
   - 3 seconds = 60 ticks
   - 5 seconds = 100 ticks
   - 60 seconds = 1,200 ticks
3. **Player Attachments:** Store ability state, timers, waypoints, saved origins, restrained entities, and cooldowns in NeoForge player Attachments. Store entity references as UUIDs, never long-lived Java entity references.
4. **Performance first:** Do not scan every player, entity, chunk, or inventory every tick. Tick only active abilities. Batch large teleport/release work.
5. **Safe cleanup:** On death, logout, forced expiration, target death/despawn, cancelled channel, or dimension change, restore movement/AI state and clear temporary references.
6. **No external UI libraries required:** Use Minecraft/NeoForge built-in `Screen`, `EditBox`, `Button`, `GuiGraphics`, vanilla font, vanilla item icons, and HUD render events.
7. **No custom visual overlay for Kamui Intangibility:** No transparent player model, shader, custom particles, or body-part materialization system.
8. **Use built-in sounds until a custom sound is provided.** Custom sound files must be `.ogg`, registered through `sounds.json`.

---

# 2. Project Structure / Current Foundation

```text
com.tobi.tobimod/
├── client/
│   ├── ClientEventHandler.java       # client-side virtual floor + keybinds
│   ├── hud/KamuiTimerHud.java        # crosshair timer renderer
│   ├── keybinds/ModKeybindings.java
│   └── screens/                      # KamuiNavigationScreen, ManualTeleport, WaypointEditor
├── common/
│   ├── abilities/
│   │   ├── KamuiIntangibilityHandler.java   # server-side virtual floor system
│   │   ├── KamuiIntangibilityState.java     # Attachment: floorY, sinkAccumulator, jumpEscapeConsumed
│   │   └── KamuiChannelHandler.java         # 3-second channel for travel/absorption
│   ├── capabilities/TobiPlayerData.java
│   └── ModAttributes.java
├── mixin/
│   ├── LivingEntityKamuiMixin.java   # noPhysics + resetFallDistance every tick
│   └── EntityKamuiMixin.java         # prevents isInWall suffocation while Kamui active
├── network/
│   ├── PacketHandler.java
│   └── payload/
│       ├── KamuiIntangibilityTogglePayload.java
│       └── KamuiIntangibilityStatePayload.java  # carries floorY (double), not underground bool
└── TobiMod.java
```

Mixin registration in `META-INF/neoforge.mods.toml`:

```toml
[[mixins]]
config="tobimod.mixins.json"
```

---

# 3. Passive Attribute Buffs

## Goal
Make the player feel physically stronger than normal Minecraft at all times.

## Intended buffs

- +20 max health: 40 HP total.
- +8 attack damage.
- +20% movement speed.
- +100% jump strength.
- 2-block step height.
- 100% knockback resistance.
- Fall damage immunity.
- Fire damage immunity and immediate extinguish.
- Permanent night vision.
- Infinite saturation/no hunger depletion.

## Implementation rules

- Use stable UUID attribute modifiers.
- Apply modifiers on join, respawn/clone, and ability/mod enable—not every tick.
- Never repeatedly call `setHealth(getMaxHealth())`; that would make the player constantly heal.
- Refresh night vision only before it expires.
- Refill hunger only when needed.
- Cancel fall/fire damage as a backup.

---

# 4. Combat Kamui Intangibility — Current Core Ability

## Keybind

```text
R = Toggle Combat Kamui Intangibility
```

## Main goal

A simplified full-body Obito ghost mode. There is no selective body-part materialization.

When safely intangible:

```text
No damage
No normal hurt registration
No hurt sound/animation/red flash where vanilla cancellation prevents it
No combat knockback
No projectile embedding
Vanilla projectiles do not affect the player
```

## Duration / cooldown — finalized

```text
Maximum duration: 60 seconds
Natural 60-second expiry cooldown: 5 seconds
Manual R toggle-off cooldown: 1 second
```

## Attack weakness — finalized

```text
Attack a real entity while Kamui is active
        ↓
Player is vulnerable for 3 ticks / 0.15 seconds
        ↓
Full Kamui protection automatically resumes
```

The player does not fully deactivate Kamui merely by attacking.

## Damage and projectile handling

Protected state means:

```text
Kamui active
AND not inside the 3-tick attack vulnerability window
```

Implementation:

- Use `EntityInvulnerabilityCheckEvent` for early damage immunity.
- Use projectile impact handling for vanilla arrows, tridents, fireballs, etc.
- Protected direct projectile contact should cancel/ignore the hit and remove/discard the projectile rather than embed it in the player.
- Modded projectiles may need compatibility patches later.

## Knockback

### Normal combat knockback
Cancel `LivingKnockBackEvent` while the player is protected.

### Explosion knockback
Explosion force may bypass `LivingKnockBackEvent`. Remove protected Kamui players from `ExplosionEvent.Detonate` affected entities so Creepers/TNT do not push them.

---

## 4.1 Virtual Floor System — Current Implementation

### Design philosophy

Obito walks through walls with normal physics — no floating, no flight, no hover. He stands on the ground, walks into a wall, and phases through it. Gravity still applies. He sinks through floors by choice (shift) and rises by jumping. **No creative flight. No `setNoGravity(true)`.**

### Core mechanism

While Kamui is active, every tick:

```text
1. noPhysics = true         → player passes through ALL blocks (walls, floors, everything)
2. Gravity stays vanilla    → player falls naturally under gravity
3. No flight                → no creative flight, no flying speed boost
4. Virtual floor at floorY  → if player.Y < floorY while falling, clamp Y = floorY, zero Y vel
5. resetFallDistance()       → no fall damage
```

The player moves with vanilla physics. Gravity pulls them down, and they "land" on the virtual floor exactly like landing on a real block. The floor just exists at `floorY`, even if it's inside solid stone.

### Jitter prevention

Gravity is applied inside `travel()`, which runs between Pre-tick and Post-tick. Without intervention, the player falls ~0.003 blocks per tick below floorY and gets clamped back, causing visible screen flicker (63.99999 → 64.0).

**Fix:** In Pre-tick, when the player is at floorY and not rising:
1. Snap Y to exactly floorY
2. Zero Y velocity
3. Set `onGround = true`
4. Temporarily set `noGravity = true` (prevents travel() from applying the -0.08 pull)

In Post-tick, immediately restore `noGravity = false` so gravity works normally for jumps and falls.

### Vertical navigation

```text
Jump (press)  → floorY += 1 (once per press, consumed flag prevents 20 raises/sec)
Shift (hold)  → floorY -= 1 every 5 ticks (continuous sinking, 0.25s per block)
Gap walk      → floorY auto-adjusts down to nearest terrain below (findSupportBelow scan)
```

**Jump detection:** Compares Y velocity before and after `travel()`. If `yVelBeforeTravel ≤ 0 && yVelAfterTravel > 0`, a jump was processed. A `jumpEscapeConsumed` flag prevents holding jump from raising floorY 20 times per second. The flag resets when the player lands back on the virtual floor.

**Sink detection:** Shift must be held AND player must be on the virtual floor (`|Y - floorY| < ε`). An accumulator counts ticks; every 5 ticks, floorY decreases by 1 and `findSupportBelow` scans for the new support level.

### Terrain support scan (`findSupportBelow`)

Scans downward from the current floorY in the block column directly below the player's center X/Z. Returns the Y of the first solid block's top surface (blockY + 1.0 for full blocks). If no solid is found within 128 blocks, keeps current floorY to prevent void fall.

### Floor initialization

On activation, `floorY = player.getY()`. Then `findSupportBelow` scans down to find the actual terrain support. This handles the case where the player activates Kamui while mid-air (floorY adjusts to the next solid below).

### Deactivation

When Kamui deactivates:
- Restore original `noPhysics`, `noGravity`, `flying`, `flyingSpeed`
- Remove any lingering `CREATIVE_FLIGHT` attribute modifier
- Clear `PRE_TRAVEL_Y_VEL` tracking map

If the player is inside solid blocks when Kamui deactivates, vanilla `isInWall()` suffocation damage kicks in naturally (the `EntityKamuiMixin` only suppresses suffocation while Kamui is active).

### Mixin architecture

```text
LivingEntityKamuiMixin:
  - Injects at tickEffects TAIL
  - Sets noPhysics = true + resetFallDistance while Kamui active
  - Server reads Attachment; client reads KamuiIntangibilityStatePayload

EntityKamuiMixin:
  - Injects at isInWall HEAD, cancellable
  - Returns false while Kamui active (prevents suffocation inside blocks)
  - Server reads Attachment; client reads KamuiIntangibilityStatePayload
```

### Client-side prediction

The client mirrors the server's virtual floor logic to keep prediction smooth:

```text
Pre-tick:  same jitter prevention (snap Y, zero Y vel, setNoGravity(true) when on floor)
Post-tick: restore noGravity(false), safety-net floor clamp
```

`KamuiIntangibilityStatePayload` carries `floorY` (double) so the client knows the exact floor level.

### What was removed (replaced by virtual floor)

```text
❌ Surface / Underground two-mode system
❌ isHeadInsideSolidBlock / hasValidSurfaceSpace mode detection
❌ updateMovementMode / surfaceClearTicks
❌ tryEnterWall() shift-nudge into walls
❌ sinkIntoFloor() vertical teleport
❌ setNoGravity(true) — gravity stays vanilla at all times
❌ Creative flight + CREATIVE_FLIGHT attribute modifier
❌ Underground flying speed constant
```

---

# 5. Kamui Indicator / Feedback

## No visual body effects

Do not use:

```text
Translucent overlay
Transparency shader
Custom particles
Sharingan particle effects
```

## Current timer HUD

Implemented. Shows a minimal timer below the crosshair while Combat Kamui is active:

```text
        +
  KAMUI • 60s
```

Rules:

- Centered horizontally.
- Approximately 12 pixels below crosshair.
- Vanilla font.
- Purple text with small translucent black background.
- No action-bar use.
- No cooldown display.
- No vulnerable-window display.
- No icons or custom textures.
- Timer number visibly changes only once per second.

### Performance

- One/two text/rectangle draw calls per rendered frame: negligible client cost.
- Server sends state only when Kamui activates/deactivates or floorY changes.
- Client counts displayed seconds down locally every 20 client ticks.
- No timer packets every tick.

## Planned dodge sound

When a protected attack genuinely attempts to hit the player:

```text
Normal hurt result is prevented
        ↓
Play Kamui dodge/phase sound for that player only
```

Use a tiny sound rate limit (for example 3–5 ticks) to avoid multi-hit sound spam.

Until the custom sound is supplied, use a quiet built-in Minecraft sound. Custom file requirements later:

```text
src/main/resources/assets/tobimod/sounds/kamui_dodge.ogg
src/main/resources/assets/tobimod/sounds.json
```

Format must be `.ogg`, not MP3/WAV.

---

# 6. Kamui Absorption / Kidnap — New Final Direction

This replaces the older large 7-block sphere idea.

## Keybind

```text
X = Kamui Absorption / Release
```

## Forward absorption area

```text
Horizontal range: 3 blocks
Vertical range: 3 blocks
Shape: 180° forward semicircle
```

Interpretation:

```text
Targets within 3 blocks
AND in the forward half around the player
AND within the 3-block vertical limit
```

Diagram:

```text
+++           ---
+++  PLAYER   ---
+++           ---

+ = outside absorption field
- = inside absorption field
```

## Valid targets

Capture every eligible mob in the field:

```text
Passive mobs: yes
Neutral mobs: yes
Hostile mobs: yes
Named mobs: yes
Tamed mobs: yes
Warden: yes
```

Only these are excluded:

```text
Ender Dragon
Wither
```

Players are not currently included in the stated target rules. Treat player capture as unconfirmed/future multiplayer work unless explicitly added.

## Cast flow

```text
Press X
        ↓
Find all valid entities in the 3-block, 180° forward field
        ↓
Caster becomes solid/vulnerable
Selected entities are restrained
        ↓
3-second absorption channel
        ↓
On success: entities are moved to Kamui Void
```

### During the 3-second channel

Caster:

```text
Cannot move
Cannot attack
Cannot use defensive Kamui
Takes normal combat damage
```

Targets:

```text
No AI
No movement
No attack
Zero velocity
```

### Interruption

Any qualifying direct combat damage to the caster:

```text
Cancels channel
Releases untransferred targets
Applies 3-second cooldown
```

## DOT rule

Do not interrupt the channel for ordinary damage-over-time effects:

```text
Poison
Wither effect
Fire tick damage
Magma/hot-floor tick damage
Similar slow periodic damage
```

Interrupt for real/direct combat damage:

```text
Melee attack
Projectile
Explosion
Direct magic attack
Direct entity attacker
```

## Cooldowns — finalized

```text
Successful absorption cooldown: 3 seconds
Interrupted absorption cooldown: 3 seconds
```

## Unlimited-target decision

There is intentionally no gameplay target cap.

However, all entity transfer/release work must use an **unbounded queue processed in batches**, e.g. 10 entities/tick.

---

# 7. Kamui Release

## Keybind

```text
Shift + X = Release
```

## Outside Kamui Void

```text
Shift + X in any non-Kamui dimension
→ release every entity currently stored in Kamui Void
```

The likely intended destination is the player's current location. This needs final confirmation before code.

Release must also use an unlimited batched queue, not all entities in one tick.

## Inside Kamui Void

```text
Shift + X inside Kamui Void
→ release entities only in the same 3-block, 180° forward field
```

---

# 8. Kamui Dimension

## Canon / privacy decision

The Kamui dimension is **shared** by anyone with the Kamui ability, which is canon-appropriate.

Primary use is single-player, so it is effectively private in normal use.

## Dimension ID

```text
tobimod:kamui_void
```

## First visual/world design

```text
Simple flat world
Gray concrete is the only normal terrain block
No structures
No natural mob spawning
No ordinary portal access
```

---

# 9. Self Kamui Absorption / Round-Trip Warp

## Current intended key

```text
C = Kamui navigation / self transfer system
```

## Self entry behavior

```text
Save exact origin:
- source dimension
- exact X/Y/Z
- yaw/pitch

Start 3-second vulnerable channel
        ↓
Teleport player to Kamui Void
```

Note: the saved **origin** keeps yaw/pitch so the round trip returns the player
facing exactly as they left. This is separate from **waypoints** (§10), which
deliberately do not store yaw/pitch.

## Return

While inside Kamui Void, choose the return/origin option:

```text
3-second vulnerable channel
        ↓
Return to saved exact origin
```

## Channel damage rule

Same as absorption:

```text
Direct combat damage cancels
DOT does not cancel
```

---

# 10. Kamui Navigation / Waypoint GUI — Locked Concept

The C system is planned as a radial/navigation GUI. It combines self warp, waypoints, and manual coordinate transfer.

## Input conflict — RESOLVED

**Final decision:** C only ever opens the wheel. C never starts a channel directly.

```text
Press/hold C → open radial wheel
Choose an option → start that option's 3-second channel
```

## Main radial wheel

Contents:

```text
Special option: Enter Kamui / Return to Origin
Center button:  "Choose Coordinates"
10 surrounding slots: saved favorite locations
```

The special option is context-sensitive:

```text
Outside tobimod:kamui_void → "Enter Kamui"
Inside  tobimod:kamui_void → "Return to Origin"
```

### Center button — CONFIRMED

The middle of the wheel is a **button labelled "Choose Coordinates"**. It does not
teleport and does not start a channel. Clicking it navigates away from the wheel
to the manual coordinate selection screen.

## Manual coordinate page

```text
┌──────────────────────────────────┐
│       MANUAL KAMUI TRANSFER       │
├──────────────────────────────────┤
│ X: [____________]                 │
│ Y: [____________]                 │
│ Z: [____________]                 │
│                                  │
│ [Teleport]           [Cancel]     │
└──────────────────────────────────┘
```

Rules:

```text
Same-dimension only
3-second vulnerable channel
Server validates coordinates/build limits/world border/safe arrival
```

## Waypoints

```text
Maximum: 10 saved favorites per player
```

Each stores exactly:

```text
Custom name
X/Y/Z
Dimension ID
```

Yaw/pitch is **not** stored. Arrival facing is whatever the player is already facing.

---

# 11. Kamui Travel

Original intended ability remains conceptually separate:

```text
G = Kamui Travel
```

Original plan:

```text
Toggle vanilla spectator mode
Fly/pass through blocks
No world interaction while in Spectator
Return to original game mode at safe location
```

This remains lower priority now that Combat Kamui, Self Kamui, and waypoints have more detailed designs.

---

# 12. Basic Genjutsu

## Keybind

```text
V = Basic Genjutsu
```

## Goal
Freeze one non-player target while maintaining eye contact.

Rules:

```text
Range: 30 blocks
One target only
Line of sight required
Target AI/movement/attacks disabled
Look away / lose sight / leave range for 1 second → release
Manual release with key
Cooldown: 3 seconds
```

Implementation:

```text
Server-side entity raycast, ideally every 2 ticks
Store original AI state
Restore original AI state exactly on release
```

---

# 13. Advanced Genjutsu

## Keybind

```text
B = Advanced Genjutsu
```

## Goal
Temporarily control one hostile/neutral mob.

Rules:

```text
Range: 30 blocks
One target
Duration: 10 seconds
Cooldown: 30 seconds
Target ignores caster
Target attacks nearby hostile mobs / follows caster loosely
Players/bosses/passive animals/already-controlled targets excluded
```

This remains the highest AI-risk ability and should be built late.

---

# 14. Black Receivers

## Keybind

```text
F = summon/despawn Black Receivers
```

## Goal
Custom personal weapon that tags targets, then drains all tagged targets.

Rules:

```text
One owned unbreakable receiver
Basic hit: 10 damage
Tag duration: 30 seconds
Right-click drain: 1/3 of each tagged target's max health
Drain cooldown: 10 seconds
Dropped receiver vanishes after 5 seconds
Cannot be stored/transferred
```

---

# 15. Keybind Map — Current / Planned

| Key | Ability | Status |
|---|---|---|
| `R` | Combat Kamui Intangibility | **Implemented — virtual floor system** |
| `X` | Kamui Absorption / Release | Designed, not yet implemented |
| `Shift + X` | Kamui Release | Designed, not yet implemented |
| `C` | Kamui Navigation / Self Warp / Waypoints | GUI design locked; not yet implemented |
| `G` | Kamui Travel | Planned |
| `V` | Basic Genjutsu | Planned |
| `B` | Advanced Genjutsu | Planned |
| `F` | Black Receivers | Early implementation exists |

All default mappings must remain rebindable in Minecraft Controls.

---

# 16. Performance Rules

## Good patterns

```text
Virtual floor: one local block column scan for findSupportBelow
One nearby mob target-clear scan only on activation
One small forward entity query only when X is cast
UUID lists for restrained/kidnapped targets
Transfer/release queues batched by tick (10 entities/tick)
Pre-load destination chunk only when a transfer actually succeeds
State payload sent only on activation/deactivation/floorY change
```

## Avoid

```text
Scanning all entities every tick
Scanning all entities in Kamui Void every tick
Teleporting unlimited mobs in one tick
Sending HUD timer packets every tick
Loading waypoint chunks just to display GUI
Repeatedly re-adding attribute modifiers
Global mob aggro reset outside the local relevant area
Block collision shape queries every tick for every nearby block
```

---

# 17. Current Implementation Status

## Working — Combat Kamui Intangibility

```text
✅ R toggle on/off
✅ 60-second duration
✅ 5-second forced-expiry cooldown
✅ 1-second manual-off cooldown
✅ Damage protection (EntityInvulnerabilityCheckEvent)
✅ 3-tick attack vulnerability on attack
✅ Combat knockback cancellation (LivingKnockBackEvent)
✅ Explosion affected-entity removal (ExplosionEvent.Detonate)
✅ Projectile cancel + discard on impact
✅ Crosshair timer HUD
✅ Virtual floor system (noPhysics + gravity + floorY clamp)
✅ No jitter (Pre-tick gravity suppression + Post-tick restore)
✅ Jump raises floorY +1 (velocity detection + consumed flag)
✅ Shift-hold sinks floorY -1 every 5 ticks
✅ Auto-adjust floorY down to terrain (findSupportBelow)
✅ No suffocation while Kamui active (EntityKamuiMixin → isInWall)
✅ No creative flight — vanilla gravity + vanilla jump only
✅ Mixin/Attachment architecture (no stealable potion effect)
✅ Client-side prediction mirrors server virtual floor
```

## Not yet implemented

```text
Custom Kamui dodge sound
Kamui Absorption / Release
Self warp round trip
C navigation GUI (screens exist as prototypes)
Waypoints
Manual coordinate teleport
Kamui Travel
Genjutsu
Full Black Receiver refinement
```

---

# 18. Open Decisions Still Needed

1. **Release destination outside Kamui Void:** Confirm all released mobs should appear at the caster's current location.
2. **Line of sight for X absorption:** The current new design defines a forward range/cone but has not explicitly finalized whether blocks can block selection. Recommended: require line of sight.
3. **Player capture:** Not included in current target plan; leave for future multiplayer-specific design.
4. **Waypoint GUI visual layout:** Radial wheel is desired, but implementation can initially use clean vanilla buttons/list behavior if the wheel becomes hard to use with 10 long labels.
5. **Custom Kamui sound:** Provide `.ogg` later; use built-in placeholder sound meanwhile.

---

# 19. Definition of Done for an Ability

An ability is complete when:

- keybind/UI request reaches server correctly;
- server validates all conditions;
- state survives/cleans up correctly as required;
- cooldown and timers work in ticks;
- death/logout/cancellation restores movement and AI state;
- no unintended item duplication/entity leaks occur;
- it is tested in a normal survival world;
- large entity operations are batched;
- no console errors occur;
- the feature does not rely on a stealable potion effect unless intentionally designed as one.
