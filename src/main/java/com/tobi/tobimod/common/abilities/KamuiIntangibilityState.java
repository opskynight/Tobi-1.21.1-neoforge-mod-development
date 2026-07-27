package com.tobi.tobimod.common.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

public final class KamuiIntangibilityState {
    public static final int MAX_DURATION_TICKS = 20 * 60;
    public static final int ATTACK_VULNERABILITY_TICKS = 3;
    public static final int EXPIRED_COOLDOWN_TICKS = 20 * 5;
    public static final int MANUAL_COOLDOWN_TICKS = 20;
    public static final int SURFACE_CONFIRMATION_TICKS = 3;

    public static final Codec<KamuiIntangibilityState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("active").forGetter(value -> value.active),
                    Codec.LONG.fieldOf("activeEndsAt").forGetter(value -> value.activeEndsAt),
                    Codec.LONG.fieldOf("vulnerabilityEndsAt").forGetter(value -> value.vulnerabilityEndsAt),
                    Codec.LONG.fieldOf("cooldownEndsAt").forGetter(value -> value.cooldownEndsAt),
                    Codec.BOOL.fieldOf("underground").forGetter(value -> value.underground),
                    Codec.INT.fieldOf("surfaceClearTicks").forGetter(value -> value.surfaceClearTicks),
                    Codec.BOOL.fieldOf("originalNoPhysics").forGetter(value -> value.originalNoPhysics),
                    Codec.BOOL.fieldOf("originalNoGravity").forGetter(value -> value.originalNoGravity),
                    Codec.BOOL.fieldOf("originalFlying").forGetter(value -> value.originalFlying),
                    Codec.FLOAT.fieldOf("originalFlyingSpeed").forGetter(value -> value.originalFlyingSpeed)
            ).apply(instance, KamuiIntangibilityState::new)
    );

    private boolean active;
    private long activeEndsAt;
    private long vulnerabilityEndsAt;
    private long cooldownEndsAt;

    private boolean underground;
    private int surfaceClearTicks;

    private boolean originalNoPhysics;
    private boolean originalNoGravity;
    private boolean originalFlying;
    private float originalFlyingSpeed;

    public KamuiIntangibilityState() {
        this(false, 0L, 0L, 0L, false, 0, false, false, false, 0.05F);
    }

    private KamuiIntangibilityState(
            boolean active,
            long activeEndsAt,
            long vulnerabilityEndsAt,
            long cooldownEndsAt,
            boolean underground,
            int surfaceClearTicks,
            boolean originalNoPhysics,
            boolean originalNoGravity,
            boolean originalFlying,
            float originalFlyingSpeed
    ) {
        this.active = active;
        this.activeEndsAt = activeEndsAt;
        this.vulnerabilityEndsAt = vulnerabilityEndsAt;
        this.cooldownEndsAt = cooldownEndsAt;
        this.underground = underground;
        this.surfaceClearTicks = surfaceClearTicks;
        this.originalNoPhysics = originalNoPhysics;
        this.originalNoGravity = originalNoGravity;
        this.originalFlying = originalFlying;
        this.originalFlyingSpeed = originalFlyingSpeed;
    }

    public boolean isActive() { return active; }
    public boolean isUnderground() { return underground; }
    public boolean isProtected(long gameTime) { return active && gameTime >= vulnerabilityEndsAt; }
    public boolean isOnCooldown(long gameTime) { return !active && gameTime < cooldownEndsAt; }
    public boolean hasExpired(long gameTime) { return active && gameTime >= activeEndsAt; }

    public void activate(Player player, long gameTime) {
        active = true;
        activeEndsAt = gameTime + MAX_DURATION_TICKS;
        vulnerabilityEndsAt = 0L;
        underground = false;
        surfaceClearTicks = 0;

        originalNoPhysics = player.noPhysics;
        originalNoGravity = player.isNoGravity();
        originalFlying = player.getAbilities().flying;
        originalFlyingSpeed = player.getAbilities().getFlyingSpeed();
    }

    /**
     * Enter immediately when head is inside terrain. Leave only after a valid
     * standing space remains clear for three ticks.
     */
    public boolean updateMovementMode(boolean headInsideSolid, boolean validSurfaceSpace) {
        boolean previous = underground;

        if (headInsideSolid) {
            underground = true;
            surfaceClearTicks = 0;
        } else if (underground) {
            if (validSurfaceSpace) {
                surfaceClearTicks++;
                if (surfaceClearTicks >= SURFACE_CONFIRMATION_TICKS) {
                    underground = false;
                    surfaceClearTicks = 0;
                }
            } else {
                surfaceClearTicks = 0;
            }
        }

        return previous != underground;
    }

    public void makeVulnerable(long gameTime) {
        if (active) {
            vulnerabilityEndsAt = gameTime + ATTACK_VULNERABILITY_TICKS;
        }
    }

    public void deactivate(long gameTime, boolean manual) {
        active = false;
        activeEndsAt = 0L;
        vulnerabilityEndsAt = 0L;
        underground = false;
        surfaceClearTicks = 0;
        cooldownEndsAt = gameTime + (manual ? MANUAL_COOLDOWN_TICKS : EXPIRED_COOLDOWN_TICKS);
    }

    public int remainingActiveSeconds(long gameTime) {
        if (!active) {
            return 0;
        }

        long remainingTicks = Math.max(0L, activeEndsAt - gameTime);
        return (int) Math.ceil(remainingTicks / 20.0D);
    }

    public boolean originalNoPhysics() { return originalNoPhysics; }
    public boolean originalNoGravity() { return originalNoGravity; }
    public boolean originalFlying() { return originalFlying; }
    public float originalFlyingSpeed() { return originalFlyingSpeed; }

    public void clearAfterDeath() {
        active = false;
        activeEndsAt = 0L;
        vulnerabilityEndsAt = 0L;
        cooldownEndsAt = 0L;
        underground = false;
        surfaceClearTicks = 0;
        originalNoPhysics = false;
        originalNoGravity = false;
        originalFlying = false;
        originalFlyingSpeed = 0.05F;
    }
}