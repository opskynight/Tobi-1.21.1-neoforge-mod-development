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

Current important project areas:

```text
com.tobi.tobimod/
├── client/
│   ├── ClientEventHandler.java
│   ├── hud/                         # planned crosshair timer renderer
│   ├── keybinds/ModKeybindings.java
│   └── screens/                     # current GUI prototypes; will be refactored
├── common/
│   ├── abilities/
│   │   ├── KamuiIntangibilityHandler.java
│   │   └── KamuiIntangibilityState.java
│   ├── capabilities/TobiPlayerData.java  # incomplete/old; redesign later
│   └── ModAttributes.java
├── mixin/LivingEntityKamuiMixin.java
├── network/
│   ├── PacketHandler.java
│   └── payload/
│       ├── KamuiIntangibilityTogglePayload.java
│       └── KamuiIntangibilityStatePayload.java
└── TobiMod.java
```

Current mixin registration is required in `META-INF/neoforge.mods.toml`:

```toml
[[mixins]]
config="tobimod.mixins.json"
```

The leading `#` comments must not be present. If the block is commented, underground no-clip does not run.

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

Implementation direction:

- Use `EntityInvulnerabilityCheckEvent` for early damage immunity.
- Use projectile impact handling for vanilla arrows, tridents, fireballs, etc.
- Protected direct projectile contact should cancel/ignore the hit and remove/discard the projectile rather than embed it in the player.
- Modded projectiles may need compatibility patches later.

## Knockback

### Normal combat knockback
Cancel `LivingKnockBackEvent` while the player is protected.

### Explosion knockback
Explosion force may bypass `LivingKnockBackEvent`. Remove protected Kamui players from `ExplosionEvent.Detonate` affected entities so Creepers/TNT do not push them.

### Surface physical pushing
Surface mode deliberately uses normal collision. Mobs physically walking into the player may still cause ordinary entity pushing. This is separate from combat/explosion knockback and can be revisited later.

---

## 4.1 Surface / Underground Movement — Final Intended Design

### Surface Mode

While Combat Kamui is active on the surface:

```text
Normal survival movement
Normal gravity
Normal jumping
Normal block collision
Normal item/block interaction
No free flight
No hovering
No standard wall/mob phasing
Kamui defensive protection remains active
```

This avoids the player looking like they have Creative/Spectator flight.

### Intentional terrain entry

The player deliberately enters terrain by holding Shift.

#### Floor entry

```text
Kamui active + Surface Mode + Shift
        ↓
If no solid wall is directly ahead:
Move player downward by 0.15 blocks/tick
        ↓
Once head enters a solid collision block:
Underground Mode begins
```

This is the mod equivalent of the old datapack idea:

```mcfunction
tp @s ~ ~-0.15 ~
```

#### Horizontal wall entry

```text
Kamui active + Surface Mode + Shift + looking at a nearby solid wall
        ↓
Nudge player forward by 0.15 blocks/tick in horizontal look direction
        ↓
Once head enters terrain:
Underground Mode begins
```

This is intentional phase entry, not automatic phasing. Normal walking into a wall should remain normal collision.

### Underground Mode

When the player’s head is inside a solid collision block:

```text
No-clip enabled
No gravity enabled
Temporary internal flight enabled
Fast movement: 2× normal Creative flight speed
WASD: horizontal movement
Space: up
Shift: down
```

The player never switches to Spectator mode.

### Exit Underground Mode

When the player reaches three clear collision blocks of surface space for 3 stable ticks:

```text
Underground flight disabled
Velocity/momentum cleared
No-clip disabled
Normal gravity/collision restored
Kamui defensive state remains active
```

A solid block directly below is not required for exit. If the player exits slightly above ground, normal gravity brings them down. This specifically prevents infinite upward flight after emerging.

### Underground mob aggro

On the one transition into Underground Mode:

```text
Find nearby mobs within 32 blocks that currently target the player
Clear their target once
```

Do not repeat this every tick. On surface return, vanilla AI can naturally reacquire the player.

## Technical implementation status

- The initial `noPhysics + noGravity` approach did not produce usable player no-clip.
- The Intangible reference mod demonstrated that timing matters.
- Hidden custom effect was used successfully as a temporary movement driver.
- Current desired architecture removes the stealable effect and uses:

```text
Attachment-only Kamui status
+ targeted LivingEntity mixin
```

The mixin applies underground no-clip at the correct entity lifecycle point.

### Important mixin architecture

```text
Server:
Read Kamui Attachment state.

Client:
Read compact `KamuiIntangibilityStatePayload` state.

Mixin:
Only applies noPhysics/noGravity while Underground Mode is true.
```

No Kamui potion/status effect should remain once the attachment-only version is complete; modded enemies cannot steal an Attachment as a potion effect.

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

## Planned timer HUD

Show only a minimal timer below the crosshair while Combat Kamui is active:

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
- Server sends state only when Kamui activates/deactivates or movement state changes.
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

For modded DOT, no universal Minecraft label exists. The robust code rule is to cancel only for direct attacker/projectile/explosion/direct combat sources, which naturally ignores most DOT effects.

## Cooldowns — finalized

```text
Successful absorption cooldown: 3 seconds
Interrupted absorption cooldown: 3 seconds
```

## Unlimited-target decision

There is intentionally no gameplay target cap.

However, all entity transfer/release work must use an **unbounded queue processed in batches**, e.g. 10 entities/tick.

Example:

```text
120 mobs captured
→ all are valid
→ after 3-second channel, move 10/tick
→ all transfer over 12 ticks
```

This is not a target limit. It prevents one server tick from doing 120 cross-dimension teleports.

During a large transfer queue, caster remains vulnerable. If interrupted:

```text
Already transferred mobs stay in Kamui Void
Remaining mobs are released
```

### Performance warning

Unlimited targets can still lag if the player intentionally uses it in an entity-crammed farm. Batching prevents a single catastrophic teleport spike but cannot make thousands of loaded mobs free.

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

The likely intended destination is the player’s current location. This needs final confirmation before code.

Release must also use an unlimited batched queue, not all entities in one tick.

## Inside Kamui Void

```text
Shift + X inside Kamui Void
→ release entities only in the same 3-block, 180° forward field
```

This permits controlled selection/release within the pocket dimension.

---

# 8. Kamui Dimension

## Canon / privacy decision

The Kamui dimension is **shared** by anyone with the Kamui ability, which is canon-appropriate.

Primary use is single-player, so it is effectively private in normal use. Multiplayer privacy/isolation can be redesigned later.

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

It is a shared holding/pocket dimension for:

```text
Absorbed entities
Self Kamui entry
Return/origin travel
```

World generation should be intentionally simple and reliable. Dimension implementation should ensure a safe spawn/arrival surface and pre-load the relevant destination chunk before transfer queues begin.

---

# 9. Self Kamui Absorption / Round-Trip Warp

## Current intended key

```text
C = Kamui navigation / self transfer system
```

## Self entry behavior

The player chooses Self Kamui entry from the planned C navigation GUI.

```text
Save exact origin:
- source dimension
- exact X/Y/Z
- yaw/pitch

Start 3-second vulnerable channel
        ↓
Teleport player to Kamui Void
```

## Return

While inside Kamui Void, choose the return/origin option:

```text
3-second vulnerable channel
        ↓
Return to saved exact origin
```

If saved location is unsafe, find a small nearby safe space; use a defined fallback only if no safe location can be found.

## Channel damage rule

Same as absorption:

```text
Direct combat damage cancels
DOT does not cancel
```

---

# 10. Kamui Navigation / Waypoint GUI — Locked Concept

The C system is planned as a radial/navigation GUI. It combines self warp, waypoints, and manual coordinate transfer.

## Important unresolved input conflict

Earlier ideas say both:

```text
Hold C to channel Self Kamui
Hold C to open a radial GUI
```

These cannot happen from the same hold at the same time.

Recommended interaction to finalize later:

```text
Press/hold C → open radial wheel
Choose an option → start that option's 3-second channel
```

This preserves the desired channel without using C for two conflicting actions.

## Main radial wheel

Contents:

```text
Special option: Enter Kamui / Return to Origin
Center option: Manual Coordinates
10 surrounding slots: saved favorite locations
```

Do not permanently cram editable X/Y/Z boxes inside a small wheel. Clicking the center opens a clean coordinate sub-screen.

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

Each stores:

```text
Custom name
X/Y/Z
Dimension ID
Optional yaw/pitch
```

### Empty slot

```text
[ + ]
```

Click:

```text
Save current standing position
Enter or edit name
```

### Filled slot

Click:

```text
Start 3-second vulnerable Kamui transfer
Teleport cross-dimension to stored location
```

Saved waypoint transfer is cross-dimensional because the player previously visited and saved that destination.

### Required management

```text
Create waypoint
Rename waypoint
Delete waypoint
Select waypoint
Teleport to waypoint
```

## GUI status

Eventually show disabled/available/cooldown state, but no need for fancy icons or external UI libraries. Vanilla item icons and built-in buttons are enough.

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

Implementation guidance:

```text
Do not rewrite every mob's goal selector.
Use a controlled state/effect plus periodic local target assignment.
Search nearest hostile target every 10 ticks, not every tick.
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

Performance rule:

```text
Store target UUID + expiry tick
Do not scan all loaded entities every tick
Recommended tag cap was previously 16, but can be revisited if desired
```

User has reported the existing percentage-health drain concept works in early testing.

---

# 15. Keybind Map — Current / Planned

| Key | Ability | Status |
|---|---|---|
| `R` | Combat Kamui Intangibility | Implemented core; ongoing polish |
| `X` | Kamui Absorption / Release | New final design, not yet implemented |
| `Shift + X` | Kamui Release | New final design, not yet implemented |
| `C` | Kamui Navigation / Self Warp / Waypoints | GUI design locked conceptually; interaction details unresolved |
| `G` | Kamui Travel | Original planned ability |
| `V` | Basic Genjutsu | Planned |
| `B` | Advanced Genjutsu | Planned |
| `F` | Black Receivers | Early implementation exists / planned refinement |
| `Z` | Old Location Marker/radial prototype | Deprecated direction; likely absorbed into C navigation GUI |
| `O` | Optional future ability dashboard | Optional future feature |

All default mappings must remain rebindable in Minecraft Controls.

---

# 16. GUI / HUD Direction

## Current priority

1. Finish the C Kamui Navigation radial/waypoint GUI concept.
2. Use the small R Kamui timer below crosshair.
3. Avoid a large global Ability Dashboard until core abilities are stable.

## Existing prototype note

Old classes such as:

```text
TobiRadialMenu.java
KamuiTravelMenu.java
```

are prototypes and contain incorrect naming/responsibility from earlier work.

Desired future refactor:

```text
AbilityRadialMenu.java          # optional quick selector
KamuiNavigationScreen.java      # C wheel/self warp/waypoints
ManualTeleportScreen.java       # X/Y/Z page
WaypointEditorScreen.java       # naming/delete/edit operations
KamuiTimerHud.java              # R ability timer below crosshair
```

---

# 17. Performance Rules Specific to New Kamui Features

## Good patterns

```text
One local block check for head/entry state
3-tick stable surface exit counter
One nearby mob target-clear scan only when entering Underground Mode
One small forward entity query only when X is cast
UUID lists for restrained/kidnapped targets
Transfer/release queues batched by tick
Pre-load destination chunk only when a transfer actually succeeds
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
```

## Queue policy

Unlimited target selection is allowed by design. Teleport/release must still batch work, for example:

```text
10 entities per tick
```

This protects TPS while preserving “capture everything in range” gameplay.

---

# 18. Current Implementation Status

## Working / tested core

```text
R toggle works
60-second duration works
5-second forced-expiry cooldown works
1-second manual-off cooldown works
Damage protection works
3-tick attack vulnerability exists
Shift terrain entry works
Underground no-clip/flight works when mixin config is enabled
Surface return can restore normal movement
Mixin/Attachment architecture replaces stealable hidden effect
```

## Recently added / requires testing confirmation

```text
Combat knockback cancellation
Explosion affected-entity removal for Creeper/TNT knockback
Crosshair 60-second timer HUD
```

## Not yet implemented

```text
Custom Kamui dodge sound
Kamui Absorption / Release
Kamui Dimension JSON/world generation
Self warp round trip
C navigation GUI
Waypoints
Manual coordinate teleport
Kamui Travel
Genjutsu
Full Black Receiver refinement
```

---

# 19. Open Decisions Still Needed

1. **C input behavior:** Exact radial GUI opening/selection behavior versus the 3-second self-warp channel.
2. **Release destination outside Kamui Void:** Confirm all released mobs should appear at the caster's current location.
3. **Line of sight for X absorption:** The current new design defines a forward range/cone but has not explicitly finalized whether blocks can block selection. Recommended: require line of sight.
4. **Player capture:** Not included in current target plan; leave for future multiplayer-specific design.
5. **Waypoint GUI visual layout:** Radial wheel is desired, but implementation can initially use clean vanilla buttons/list behavior if the wheel becomes hard to use with 10 long labels.
6. **Custom Kamui sound:** Provide `.ogg` later; use built-in placeholder sound meanwhile.

---

# 20. Definition of Done for an Ability

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

---

**Document status:** Current consolidated design and implementation plan.  
**Last update:** Combat Kamui core has been tested and works; Kamui Absorption, Kamui Void, and C navigation GUI are the next major planning/implementation systems.
