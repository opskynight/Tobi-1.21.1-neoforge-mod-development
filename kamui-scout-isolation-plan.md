# Kamui Scout + Kamui Phase Aggro — Plan

**Status:** PLAN ONLY. No code in this pass.  
**Target:** 1.21.1 / NeoForge 21.1.218  
**No git commit until you test a later copy/paste patch.**

Locked from you:

- Scroll bug = **WASD stay the same speed** after scrolling. Space/Shift (up/down) *does* change.
- Self view (scout) = **ghost**: faint/translucent body in F5. Others see nothing.
- **Do not** tick-loop villagers to reset look. That idea is dropped.
- **Scout vanish:** `ServerPlayer#isSpectator()` mixin. If you are **already** in scout, nothing looks at you / targets you in the first place. No look-reset loop.
- **Kamui phase chase:** fully inside 2 solid blocks → drop aggro + stop paths. When you reappear (upper body / off the ground), we do **not** force old attackers back. Only mobs that **see you again** and are in **vanilla aggro range** become hostile.

---

## 1. What you already have vs what is missing

| Want | Current | Gap |
|---|---|---|
| No attack | `AttackEntityEvent` + client mouse/key cancel | Entity-interact still open (`EntityInteract` / `EntityInteractSpecific` only blocked while *channeling*, not while scouting) |
| No being attacked | invuln + damage cancel + projectile discard + knockback + explosion filter | Good enough. Keep. |
| No look / perceive | `LivingChangeTargetEvent` + `setInvisible(true)` | **Villagers still look.** Invis does not matter. 1.21 checks `isSpectator()`. |
| No world interact | block/item click + break + inventory screen cancel | Drop (Q), entity use, pressure plates, portals not explicitly covered |
| No item pickup | nothing | `ItemEntityPickupEvent` unused |
| No XP pickup | nothing | `PlayerXpEvent.PickupXp` unused |
| Orbs/items fly toward you | nothing | XP magnet uses nearest non-spectator player |
| True invis / ghost self | `setInvisible(true)` | Others: OK-ish. **You** also vanish in F5 because `isInvisibleTo(self)` is true. Hands also hide. Need a self-ghost exception. |
| Scroll fly speed on WASD | only `abilities.flyingSpeed` is written | Vertical path reads that field. Horizontal path often does **not**. |

Still not using spectator **game mode**. Game type stays Survival.

---

## 2. Perception (why villagers look)

1.21.1 does **not** use invisibility for “can this mob notice you”.

```text
LivingEntity.canBeSeenByAnyone()     = !isSpectator() && isAlive()
TargetingConditions (LookAtPlayerGoal, tempt, flee)
Villager brain SetEntityLookTarget   = type == PLAYER && !isSpectator()
EntitySelector.NO_SPECTATORS         = !isSpectator()
```

`LivingChangeTargetEvent` only fires for **attack** targets (`Mob.setTarget` / `StartAttacking`). Villager head-turn never posts it.

`ServerPlayer.isSpectator()` is:

```text
return this.gameMode.isSpectator();   // Survival → false
```

and it **overrides** `Player.isSpectator()` without calling super. A mixin on `Player` would miss the server player. A mixin on `LocalPlayer` would flip hotbar / spectator hotkeys. We will not do either.

**Plan:** mixin **`ServerPlayer#isSpectator()` only**. If scout attachment is active, return `true`. GameType stays Survival. Client `LocalPlayer#isSpectator()` stays false.

That one hook also gives spectator-like:

- no villager / cow / golem look
- no aggro / tempt / flee
- no sculk / warden hear (vibration listeners skip spectators)
- no item/XP magnet
- `isPickable()` false
- pressure plates / tripwires / most “player stepped here” checks

On scout **enter**, also wipe nearby brain memories (`LOOK_TARGET`, `NEAREST_VISIBLE_PLAYER`, `ATTACK_TARGET`, `ANGRY_AT`, …). Otherwise a villager that already stored you keeps staring until the memory expires.

Keep the existing `LivingChangeTargetEvent` cancel as backup.

Do **not** mixin `Player` / `LocalPlayer` / global `Entity#isSpectator()`.

---

## 3. True invisibility + ghost self

`setInvisible(true)` stays. It is already synced.

Vanilla F5 math (`LivingEntityRenderer`):

```text
bodyVisible  = !isInvisible()                          → false (flag is on)
translucent  = !bodyVisible && !isInvisibleTo(viewer)
render type  = translucent ? ghost : bodyVisible ? solid : nothing
```

`Entity.isInvisibleTo(self)` with the flag on returns **true** (`player != this` fails, then it returns `isInvisible()`). So F5 currently draws **nothing**. That is why you do not get a ghost.

**Plan:**

| Viewer | Result |
|---|---|
| Another player / mob | `isInvisibleTo` → true. No model. |
| You, F5 | mixin `isInvisibleTo`: if scout and viewer == self → **false**. Renderer then picks the translucent pass. Ghost body. |
| You, first person | vanilla hides arms when `isInvisible()`. Small client mixin / inject on hand renderer so local scout still draws arms. Solid hands (not required to be ghost). |

No client `isSpectator()`. No spectator hotbar. No number-key camera.

Optional later: armor/held-item layers in F5 also go through the same translucent buffer. If a layer stays opaque after the first test, we patch that layer. Not in the first cut unless it shows up.

---

## 4. No world interaction (full isolation)

Server, while scout active — add the missing cancels next to the ones you already have:

| Hook | Why |
|---|---|
| `PlayerInteractEvent.EntityInteract` | punch/use villagers, item frames, armor stands |
| `PlayerInteractEvent.EntityInteractSpecific` | same, precise hitbox |
| `ItemEntityPickupEvent.Pre` → `setCanPickup(FALSE)` | ground items |
| `PlayerXpEvent.PickupXp` cancel | XP orbs |
| `ItemTossEvent` cancel | Q / drop |
| `EntityItemPickup` already covered by Pre | — |
| `PlayerInteractEvent.RightClickEmpty` cancel | leftover use |

Already present, keep: attack, left/right block, right-click item, use-on-block, break, incoming damage, projectile, knockback, explosion, client `InteractionKeyMappingTriggered`, client LMB/RMB, inventory/container screen block.

`ServerPlayer.isSpectator()` mixin covers most vanilla “does this player count as present” checks (plates, tripwire, XP/item seek, many `playerTouch` early-outs). The events above stay anyway so a mod that ignores `isSpectator()` still cannot feed you items/XP.

**Portals:** vanilla spectators do not take nether/end portals. Default in this plan: **same** (you fly through the block, you do not teleport). Say if you want scout to still use portals.

**Not in scope unless you ask:** command blocks, `/give`, hopper minecarts pushing into you (noPhysics already skips most collision).

---

## 5. Horizontal scroll-speed bug (the real one)

Your scroll handler **does run**. It writes `abilities.flyingSpeed` and the HUD number changes. Vertical flight then changes because 1.21 `LocalPlayer` does this every tick:

```text
// jump / sneak only
delta.y += ± abilities.getFlyingSpeed() * 3.0
```

WASD does **not** use that line.

Horizontal goes through `LivingEntity.travel` → `moveRelative`:

```text
onGround == true  →  uses walk getSpeed()  (Attributes.MOVEMENT_SPEED ≈ 0.1)
onGround == false →  uses getFlyingSpeed() (abilities.flyingSpeed, the scrolled value)
```

Vanilla spectator forces this every tick in `Player.tick`:

```text
this.noPhysics = this.isSpectator();
if (this.isSpectator()) this.setOnGround(false);
```

On the **client**, `LocalPlayer.isSpectator()` is still false (we will not change that). So `onGround` is **not** forced false. If it is true — or if travel takes the ground friction path — WASD is locked to walk speed. You can scroll `flyingSpeed` from 0.02 to 0.50 and only Space/Shift notices.

That matches what you reported.

**Plan (all three, cheap, in this order):**

1. **Force `setOnGround(false)` every scout tick** on client *and* server (Pre + Post), same as spectator. This is the actual fix.
2. **Keep writing `abilities.setFlyingSpeed(scoutSpeed)`** as you already do (client prediction + server authority).
3. **Mixin `Player#getFlyingSpeed()`** (the LivingEntity override, not the abilities getter): if scout, return `scoutSpeed * (sprinting ? 2 : 1)`. This is the method `travel()` reads when airborne. Makes WASD and Space/Shift share one number even if some tick order flips `onGround`.

Do **not** add a `MOVEMENT_SPEED` attribute modifier. That would also make you a rocket on the ground after exit if cleanup missed.

Do **not** touch `getScrollDeltaX()`. You confirmed this is not a sideways-wheel problem. Vertical wheel stays as-is (`getScrollDeltaY()`).

Middle-click reset stays.

---

## 5b. Kamui phase — bury to drop chase, peek to get it back

This is **not** scout. Combat Kamui stays visible. We only change **aggro**, not villager look.

You already classify blocks every Kamui tick:

```text
isFeetInsideSolid  = collision/water at feet (Y)
isBodyInsideSolid  = collision/water at waist (Y+1)   // existing "underground"
```

Player is 2 blocks tall. Your rule maps cleanly:

```text
fullyBuried  = feet solid AND body solid     // both halves inside → 2 blocks
peeking      = body NOT solid                // upper 1 block in air → chase allowed
```

| State | Mobs |
|---|---|
| Fully buried (2/2 inside) | Cannot **keep** or **gain** you as `Mob.getTarget()`. Existing chases cleared **once** on the transition. |
| Upper body showing (1 block air) | Vanilla targeting works again. We do **not** force nearby mobs to notice you. |

### Cost (why this is cheap)

| When | Work |
|---|---|
| Every Kamui tick | 1 extra block check (`isFeetInsideSolid`). You already do `isBodyInsideSolid`. ~nanoseconds. |
| Transition **into** fully buried | One 32-block `Mob` box (you already have this helper). `setTarget(null)` + `getNavigation().stop()` + clear `lastHurtByMob` only on mobs that have you. Once, not every tick. |
| While buried | `LivingChangeTargetEvent` cancel. Fires **only when a mob tries to target you**, not every tick, not every mob. |
| Transition **out** (peek / surface) | Stop cancelling. Zero extra work. Vanilla AI retargets if it has range + LOS. |

No villager look wipe. No per-tick mob scan. No `isSpectator()` on Kamui (that would also stop look/tempt and is more than you asked).

### Why one-shot clear must also `navigation.stop()`

Your current `clearNearbyMobAggro` only `setTarget(null)`. Many mobs keep walking the last path. That is why they still “follow” you after you sink. We add `getNavigation().stop()` on that same one-shot.

### Emerge / LOS caveat

If you peek in a 1×1 hole, vanilla line-of-sight may still fail. We will **not** ping every nearby mob “he’s back.” That would be the scan you do not want. A zombie in front of the hole with LOS will retarget on its next target search (usually immediately).

### Not included unless you ask

- Villagers stop looking while buried
- Remember who was chasing and force them back on peek
- Scout uses this bury rule (scout is flight; this is Kamui only)

---

## 6. File plan (next pass, after you say go)

No files will be edited until you approve this plan.

| File | Change |
|---|---|
| **ADD** `KamuiScoutPerception.java` | one `isUndetectable(Entity)` helper (server attachment / client payload) |
| **ADD** `PlayerScoutMixin.java` | `ServerPlayer#isSpectator()` HEAD → true if scout |
| **ADD** `PlayerScoutFlySpeedMixin.java` | `Player#getFlyingSpeed()` HEAD → scout speed if scout |
| **ADD** `EntityScoutInvisibleMixin.java` *or* inject into existing `EntityKamuiMixin` | `isInvisibleTo(Player)` : self + scout → false (ghost); other + scout → true |
| **ADD** small client mixin on hand renderer | first-person arms still draw for local scout |
| **EDIT** `tobimod.mixins.json` | register the new mixins (`mixins` + `client`) |
| **EDIT** `KamuiScoutHandler.java` | isolation events (pickup / XP / toss / entity-interact); `setOnGround(false)` on server tick. **No** per-tick look reset. |
| **EDIT** `KamuiIntangibilityHandler.java` | `fullyBuried` = feet+body solid; on enter-buried: clear target + stop nav; while buried: cancel `LivingChangeTargetEvent` |
| **EDIT** `ClientEventHandler.java` | `setOnGround(false)` on scout client tick |
| **EDIT** `kamui-scout-undetectable.md` | replace with the real copy/paste patch once you approve |

`PlayerScoutMixin` (`isSpectator`) is **in**. Scout = already vanished; nothing acquires look/target on you.

`KamuiScoutState.java` — no schema change.

---

## 7. What we will not do

- `setGameMode(SPECTATOR)`
- mixin `LocalPlayer#isSpectator()` / `Player#isSpectator()`
- hide the hotbar or switch to spectator camera
- change scroll to read `getScrollDeltaX()`
- apply walk-speed attributes to fake fly speed
- commit anything

---

## 8. Test list (for the later patch)

1. Scout + walk up to a villager → no head turn, no greet.
2. Zombie / cow / wheat tempt → ignore you.
3. Drop a diamond, walk over it → stays on the ground. Leave scout → you can pick it up.
4. XP orb → does not fly to you, does not collect. Leave scout → it does.
5. Punch / right-click villager / item frame / chest / Q-drop → nothing.
6. Take a creeper explosion / arrow → no damage, no knockback.
7. F5 → faint ghost body. Another account (or mob) → cannot see you.
8. First person → hands still there.
9. Scroll up several ticks → WASD **and** Space both faster. Scroll down → both slower. Middle click → both back to default.
10. F3 game mode still Survival. HUD / scout overlay / flight still work.
11. Exit scout → walk speed normal, mobs see you, pickups work, body solid.
12. Kamui R, sink until both body blocks are stone → chasing zombies stop and do not repath to you.
13. Peek so the upper block is air → they can target you again if they have LOS.
14. Stand in a 1-block-thin wall (only waist inside) → still targetable (not fully buried).

---

## 9. Open default (change if you want)

- **Portals:** scout does not teleport (spectator parity). Reply if you want to keep nether/end travel while scouting.

Reply **go** (or change the portal default) and I will write the copy/paste `.md` only — still no git commit.
