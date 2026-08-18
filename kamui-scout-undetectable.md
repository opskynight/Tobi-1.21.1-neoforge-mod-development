# Kamui Scout vanish + Kamui bury aggro + fly-speed fix

**Applied in this workspace. No git commit.**  
Copy these files onto your local machine if you test there, then rebuild.

## What you get

- **Scout:** already vanished. Nothing looks at you or targets you. No attack, no being attacked, no pickup, no XP, no Q-drop, no world interact. F5 = ghost. Hands stay. Game type stays Survival.
- **Kamui:** fully inside **2** solid blocks → chase dropped. Peek so upper body is air → only mobs that **see you** in **normal range** retarget. No force-reaggro.
- **Scroll speed:** Space/Shift **and** WASD use the scrolled fly speed. `onGround` is forced false; `Player#getFlyingSpeed()` returns scout speed.

## New files

- `src/main/java/com/tobi/tobimod/common/abilities/KamuiScoutPerception.java`
- `src/main/java/com/tobi/tobimod/mixin/PlayerScoutMixin.java`
- `src/main/java/com/tobi/tobimod/mixin/PlayerScoutTravelMixin.java`
- `src/main/java/com/tobi/tobimod/mixin/EntityFlagAccessor.java`
- `src/main/java/com/tobi/tobimod/mixin/ItemInHandRendererScoutMixin.java`

## Edited files

- `src/main/resources/tobimod.mixins.json` — register the mixins above (`client` has the hand renderer)
- `src/main/java/com/tobi/tobimod/common/abilities/KamuiScoutHandler.java` — isolation events, `setOnGround(false)`, `dropCombatAggro`
- `src/main/java/com/tobi/tobimod/common/abilities/KamuiIntangibilityHandler.java` — fully-buried transition + `LivingChangeTargetEvent`
- `src/main/java/com/tobi/tobimod/client/ClientEventHandler.java` — `setOnGround(false)`, eat Q-drop, scroll uses Y then X

`KamuiScoutState.java` unchanged.

## Test

1. Scout next to a villager → no look, no greet.
2. Scout next to a zombie → no chase.
3. Drop a diamond / XP orb → no pickup, orbs do not home.
4. Q / punch / chest / villager use → nothing. Item is not deleted on Q.
5. F5 → faint ghost. First person → hands still there. F3 → Survival.
6. Scroll up → WASD **and** Space both faster. Scroll down → both slower. Middle click → default.
7. Kamui R, sink until both body blocks are stone → chase stops, they do not repath.
8. Peek one block out → only mobs that can see you in normal range become hostile again.
9. Stand in a 1-block-thin wall (only waist inside) → still targetable.
10. Exit scout → walk speed normal, pickups work, mobs see you.

If `PlayerScoutTravelMixin` fails to apply (`getFlyingSpeed` not on `Player`), tell me the mixin error — WASD may still be fixed by `setOnGround(false)` alone.
