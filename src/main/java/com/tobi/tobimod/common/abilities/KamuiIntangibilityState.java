package com.tobi.tobimod.common.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

/**
 * State attachment for Kamui intangibility.
 *
 * <p>The virtual-floor system replaces the old surface/underground two-mode
 * design.  While Kamui is active the player has {@code noPhysics = true}
 * (passes through all blocks) but a virtual floor at {@link #floorY} prevents
 * them from falling into the void.  Vanilla gravity, no flight.
 *
 * <ul>
 *   <li>Shift-hold: continuously sinks {@code floorY} downward (1 block / 5 ticks).</li>
 *   <li>Jump while inside terrain: raises {@code floorY} by 1 (escape upward).</li>
 *   <li>Walking over a gap: floor auto-adjusts down to nearest terrain.</li>
 * </ul>
 */
public final class KamuiIntangibilityState {
    public static final int MAX_DURATION_TICKS = 20 * 60;
    public static final int ATTACK_VULNERABILITY_TICKS = 3;
    public static final int EXPIRED_COOLDOWN_TICKS = 20 * 5;
    public static final int MANUAL_COOLDOWN_TICKS = 20;

    public static final Codec<KamuiIntangibilityState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("active").forGetter(value -> value.active),
                    Codec.LONG.fieldOf("activeEndsAt").forGetter(value -> value.activeEndsAt),
                    Codec.LONG.fieldOf("vulnerabilityEndsAt").forGetter(value -> value.vulnerabilityEndsAt),
                    Codec.LONG.fieldOf("cooldownEndsAt").forGetter(value -> value.cooldownEndsAt),
                    Codec.DOUBLE.fieldOf("floorY").forGetter(value -> value.floorY),
                    Codec.INT.fieldOf("sinkAccumulator").forGetter(value -> value.sinkAccumulator),
                    Codec.BOOL.fieldOf("jumpEscapeConsumed").forGetter(value -> value.jumpEscapeConsumed),
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

    /** The Y level of the virtual floor. The player stands here. */
    private double floorY;
    /** Ticks accumulated while shift is held (for continuous sinking). */
    private int sinkAccumulator;
    /** True if we already raised floorY for the current jump press. Prevents 20 raises/sec while holding jump. */
    private boolean jumpEscapeConsumed;

    private boolean originalNoPhysics;
    private boolean originalNoGravity;
    private boolean originalFlying;
    private float originalFlyingSpeed;

    public KamuiIntangibilityState() {
        this(false, 0L, 0L, 0L, 0.0, 0, false, false, false, false, 0.05F);
    }

    private KamuiIntangibilityState(
            boolean active,
            long activeEndsAt,
            long vulnerabilityEndsAt,
            long cooldownEndsAt,
            double floorY,
            int sinkAccumulator,
            boolean jumpEscapeConsumed,
            boolean originalNoPhysics,
            boolean originalNoGravity,
            boolean originalFlying,
            float originalFlyingSpeed
    ) {
        this.active = active;
        this.activeEndsAt = activeEndsAt;
        this.vulnerabilityEndsAt = vulnerabilityEndsAt;
        this.cooldownEndsAt = cooldownEndsAt;
        this.floorY = floorY;
        this.sinkAccumulator = sinkAccumulator;
        this.jumpEscapeConsumed = jumpEscapeConsumed;
        this.originalNoPhysics = originalNoPhysics;
        this.originalNoGravity = originalNoGravity;
        this.originalFlying = originalFlying;
        this.originalFlyingSpeed = originalFlyingSpeed;
    }

    // ── Accessors ───────────────────────────────────

    public boolean isActive() { return active; }
    public boolean isProtected(long gameTime) { return active && gameTime >= vulnerabilityEndsAt; }
    public boolean isOnCooldown(long gameTime) { return !active && gameTime < cooldownEndsAt; }
    public boolean hasExpired(long gameTime) { return active && gameTime >= activeEndsAt; }

    public double floorY() { return floorY; }
    public void floorY(double floorY) { this.floorY = floorY; }

    public int sinkAccumulator() { return sinkAccumulator; }
    public void sinkAccumulator(int ticks) { this.sinkAccumulator = ticks; }

    public boolean jumpEscapeConsumed() { return jumpEscapeConsumed; }
    public void jumpEscapeConsumed(boolean consumed) { this.jumpEscapeConsumed = consumed; }

    public boolean originalNoPhysics() { return originalNoPhysics; }
    public boolean originalNoGravity() { return originalNoGravity; }
    public boolean originalFlying() { return originalFlying; }
    public float originalFlyingSpeed() { return originalFlyingSpeed; }

    // ── Lifecycle ───────────────────────────────────

    public void activate(Player player, long gameTime) {
        active = true;
        activeEndsAt = gameTime + MAX_DURATION_TICKS;
        vulnerabilityEndsAt = 0L;
        floorY = player.getY();
        sinkAccumulator = 0;
        jumpEscapeConsumed = false;

        originalNoPhysics = player.noPhysics;
        originalNoGravity = player.isNoGravity();
        originalFlying = player.getAbilities().flying;
        originalFlyingSpeed = player.getAbilities().getFlyingSpeed();
    }

    public void deactivate(long gameTime, boolean manual) {
        active = false;
        activeEndsAt = 0L;
        vulnerabilityEndsAt = 0L;
        floorY = 0.0;
        sinkAccumulator = 0;
        jumpEscapeConsumed = false;
        cooldownEndsAt = gameTime + (manual ? MANUAL_COOLDOWN_TICKS : EXPIRED_COOLDOWN_TICKS);
    }

    public void makeVulnerable(long gameTime) {
        if (active) {
            vulnerabilityEndsAt = gameTime + ATTACK_VULNERABILITY_TICKS;
        }
    }

    public int remainingActiveSeconds(long gameTime) {
        if (!active) {
            return 0;
        }

        long remainingTicks = Math.max(0L, activeEndsAt - gameTime);
        return (int) Math.ceil(remainingTicks / 20.0D);
    }

    public void clearAfterDeath() {
        active = false;
        activeEndsAt = 0L;
        vulnerabilityEndsAt = 0L;
        cooldownEndsAt = 0L;
        floorY = 0.0;
        sinkAccumulator = 0;
        jumpEscapeConsumed = false;
        originalNoPhysics = false;
        originalNoGravity = false;
        originalFlying = false;
        originalFlyingSpeed = 0.05F;
    }
}