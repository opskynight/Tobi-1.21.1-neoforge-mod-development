package com.tobi.tobimod.common.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

/**
 * Server-authoritative state for Kamui Scout — spectator-like noclip flight.
 * <p>
 * Infinite duration until manual exit (no timer). Stores original physics flags
 * so they can be restored exactly. Flight speed is stored here and synced to
 * the client for scroll control.
 */
public final class KamuiScoutState {
    public static final float DEFAULT_SCOUT_SPEED = 0.05F;
    public static final float MIN_SCOUT_SPEED = 0.02F;
    public static final float MAX_SCOUT_SPEED = 0.50F;
    public static final float SCOUT_SPEED_STEP = 0.02F;

    // cooldown after manual exit to prevent spam (ticks)
    public static final int EXIT_COOLDOWN_TICKS = 20;

    public static final Codec<KamuiScoutState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("active").forGetter(v -> v.active),
                    Codec.LONG.fieldOf("cooldownEndsAt").forGetter(v -> v.cooldownEndsAt),
                    Codec.BOOL.fieldOf("originalNoPhysics").forGetter(v -> v.originalNoPhysics),
                    Codec.BOOL.fieldOf("originalNoGravity").forGetter(v -> v.originalNoGravity),
                    Codec.BOOL.fieldOf("originalFlying").forGetter(v -> v.originalFlying),
                    Codec.FLOAT.fieldOf("originalFlyingSpeed").forGetter(v -> v.originalFlyingSpeed),
                    Codec.FLOAT.fieldOf("scoutSpeed").forGetter(v -> v.scoutSpeed),
                    Codec.BOOL.fieldOf("originalInvisible").forGetter(v -> v.originalInvisible)
            ).apply(instance, KamuiScoutState::new)
    );

    private boolean active;
    private long cooldownEndsAt;

    private boolean originalNoPhysics;
    private boolean originalNoGravity;
    private boolean originalFlying;
    private float originalFlyingSpeed;
    private boolean originalInvisible;

    private float scoutSpeed;

    public KamuiScoutState() {
        this(false, 0L, false, false, false, 0.05F, DEFAULT_SCOUT_SPEED, false);
    }

    private KamuiScoutState(
            boolean active,
            long cooldownEndsAt,
            boolean originalNoPhysics,
            boolean originalNoGravity,
            boolean originalFlying,
            float originalFlyingSpeed,
            float scoutSpeed,
            boolean originalInvisible
    ) {
        this.active = active;
        this.cooldownEndsAt = cooldownEndsAt;
        this.originalNoPhysics = originalNoPhysics;
        this.originalNoGravity = originalNoGravity;
        this.originalFlying = originalFlying;
        this.originalFlyingSpeed = originalFlyingSpeed;
        this.scoutSpeed = scoutSpeed;
        this.originalInvisible = originalInvisible;
    }

    public boolean isActive() { return active; }
    public boolean isOnCooldown(long gameTime) { return !active && gameTime < cooldownEndsAt; }
    public float scoutSpeed() { return scoutSpeed; }
    public void scoutSpeed(float speed) { this.scoutSpeed = Math.clamp(speed, MIN_SCOUT_SPEED, MAX_SCOUT_SPEED); }

    public boolean originalNoPhysics() { return originalNoPhysics; }
    public boolean originalNoGravity() { return originalNoGravity; }
    public boolean originalFlying() { return originalFlying; }
    public float originalFlyingSpeed() { return originalFlyingSpeed; }
    public boolean originalInvisible() { return originalInvisible; }

    public void activate(Player player, long gameTime) {
        active = true;
        // cooldownEndsAt stays 0 while active
        originalNoPhysics = player.noPhysics;
        originalNoGravity = player.isNoGravity();
        originalFlying = player.getAbilities().flying;
        originalFlyingSpeed = player.getAbilities().getFlyingSpeed();
        originalInvisible = player.isInvisible();
        scoutSpeed = DEFAULT_SCOUT_SPEED;
    }

    public void deactivate(long gameTime, boolean manual) {
        active = false;
        if (manual) {
            cooldownEndsAt = gameTime + EXIT_COOLDOWN_TICKS;
        } else {
            cooldownEndsAt = 0L;
        }
        // keep original* and scoutSpeed for debugging but inactive
    }

    public long cooldownEndsAt() { return cooldownEndsAt; }

    public void clearAfterDeath() {
        active = false;
        cooldownEndsAt = 0L;
        originalNoPhysics = false;
        originalNoGravity = false;
        originalFlying = false;
        originalFlyingSpeed = 0.05F;
        originalInvisible = false;
        scoutSpeed = DEFAULT_SCOUT_SPEED;
    }
}
