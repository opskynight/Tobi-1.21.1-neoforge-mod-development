package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side handler for Kamui intangibility using the <b>virtual floor</b> system.
 *
 * <p>While Kamui is active:
 * <ul>
 *   <li>{@code noPhysics = true} — the player passes through all blocks.</li>
 *   <li>Vanilla gravity is enabled — the player falls naturally.</li>
 *   <li>No creative flight — no flight advantage.</li>
 *   <li>A virtual floor at {@code floorY} catches the player, preventing void death.</li>
 * </ul>
 *
 * <p>Vertical navigation:
 * <ul>
 *   <li>Jump → floorY rises 1 block (once per press).</li>
 *   <li>Hold shift → floorY sinks 1 block every 5 ticks.</li>
 *   <li>Walk over a gap → floorY auto-adjusts down to nearest terrain.</li>
 * </ul>
 *
 * <h3>Jitter prevention</h3>
 * <p>In Pre-tick, when the player is on the virtual floor and not rising,
 * we temporarily disable gravity and zero Y velocity so {@code travel()}
 * doesn't apply the -0.08 downward pull that causes per-tick flicker.
 * Gravity is restored in Post-tick.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiIntangibilityHandler {
    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_temporary_flight");

    /** Ticks of shift-hold required to sink one block. */
    private static final int SINK_TICKS_PER_BLOCK = 5;
    /** Maximum blocks to scan downward when searching for terrain support. */
    private static final int MAX_SUPPORT_SCAN_DEPTH = 128;
    /** Radius for clearing mob aggro on activation. */
    private static final double AGGRO_CLEAR_RADIUS = 32.0D;
    /** Epsilon for floor-level comparisons. */
    private static final double FLOOR_EPSILON = 0.05D;

    /**
     * Y velocity recorded in Pre-tick (after we zero it for jitter prevention).
     * Used in Post-tick to detect if travel() applied a jump.
     */
    private static final Map<UUID, Double> PRE_TRAVEL_Y_VEL = new HashMap<>();

    private KamuiIntangibilityHandler() {}

    // ════════════════════════════════════════════════
    //  Network
    // ════════════════════════════════════════════════

    public static void handleTogglePayload(KamuiIntangibilityTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                toggle(player);
            }
        });
    }

    // ════════════════════════════════════════════════
    //  Activation / Deactivation
    // ════════════════════════════════════════════════

    private static void toggle(ServerPlayer player) {
        long now = player.level().getGameTime();
        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);

        if (state.isActive()) {
            deactivate(player, state, now, true);
            return;
        }
        if (state.isOnCooldown(now)) {
            return;
        }

        state.activate(player, now);

        // Scan down to find actual support in case player is mid-air
        double supportedFloor = findSupportBelow(player, state.floorY());
        state.floorY(supportedFloor);

        applyKamuiMode(player);
        clearNearbyMobAggro(player);
        syncState(player, state);
    }

    static void deactivate(Player player, KamuiIntangibilityState state, long now, boolean manual) {
        if (!state.isActive()) {
            return;
        }

        // Restore original physics
        player.noPhysics = state.originalNoPhysics();
        player.setNoGravity(state.originalNoGravity());

        // Remove any lingering flight modifier
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = state.originalFlying();
        player.getAbilities().setFlyingSpeed(state.originalFlyingSpeed());
        state.deactivate(now, manual);

        PRE_TRAVEL_Y_VEL.remove(player.getUUID());

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
            syncState(serverPlayer, state);
        }
    }

    /**
     * Applies the Kamui virtual-floor mode: noPhysics, vanilla gravity, no flight.
     */
    private static void applyKamuiMode(ServerPlayer player) {
        player.noPhysics = true;

        // Remove any lingering flight modifier (safety)
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    // ════════════════════════════════════════════════
    //  Virtual Floor — Pre-Tick (before travel)
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) {
            return;
        }

        double floorY = state.floorY();
        double yVel = player.getDeltaMovement().y;

        // Ensure noPhysics is set before travel runs
        player.noPhysics = true;

        // Record Y velocity before we potentially zero it,
        // so Post-tick can detect if a jump was initiated.
        PRE_TRAVEL_Y_VEL.put(player.getUUID(), yVel);

        // If at or below floorY and not rising → stand on virtual floor
        if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
            player.setPos(player.getX(), floorY, player.getZ());
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            player.setOnGround(true);
            // Temporarily disable gravity this tick so travel() doesn't
            // apply the -0.08 pull that causes flicker.
            player.setNoGravity(true);
        }
    }

    // ════════════════════════════════════════════════
    //  Virtual Floor — Post-Tick (after travel)
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) {
            PRE_TRAVEL_Y_VEL.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        if (state.hasExpired(now)) {
            deactivate(player, state, now, false);
            return;
        }

        // ── Restore gravity (disabled in Pre-tick for jitter prevention) ──
        player.setNoGravity(false);

        // ── Ensure noPhysics stays set ──────────────
        player.noPhysics = true;

        // ── Auto-adjust floor down if no support ────
        double currentFloor = state.floorY();
        double supportedFloor = findSupportBelow(player, currentFloor);
        if (supportedFloor < currentFloor) {
            state.floorY(supportedFloor);
        }

        // ── Shift: continuous sinking ───────────────
        if (player.isShiftKeyDown() && isOnVirtualFloor(player, state)) {
            int acc = state.sinkAccumulator() + 1;
            if (acc >= SINK_TICKS_PER_BLOCK) {
                acc = 0;
                double newFloor = state.floorY() - 1.0D;
                double newSupported = findSupportBelow(player, newFloor);
                state.floorY(newSupported);
            }
            state.sinkAccumulator(acc);
        } else {
            state.sinkAccumulator(0);
        }

        // ── Jump: raise floor ───────────────────────
        //    Detect jump by comparing Y velocity before and after travel().
        //    Pre-tick recorded yVel before zeroing. After travel, if yVel
        //    went positive, a jump was processed.
        //    Jump always raises floorY by 1 when standing on the virtual
        //    floor — this is the vertical navigation system.
        double yVelAfterTravel = player.getDeltaMovement().y;
        double yVelBeforeTravel = PRE_TRAVEL_Y_VEL.getOrDefault(player.getUUID(), 0.0D);

        // A jump happened if: before travel the player wasn't rising (≤0),
        // and after travel they are rising (>0). This means travel()
        // processed the jump input and applied upward velocity.
        boolean jumpJustHappened = yVelBeforeTravel <= 0.0D && yVelAfterTravel > 0.0D;

        if (jumpJustHappened && !state.jumpEscapeConsumed()) {
            state.floorY(state.floorY() + 1.0D);
            state.jumpEscapeConsumed(true);
        }

        // Reset consumed flag once the player lands back on the virtual
        // floor (yVel ≤ 0 and at floorY). This allows the next jump
        // press to raise floorY again.
        if (state.jumpEscapeConsumed() && yVelAfterTravel <= 0.0D && isOnVirtualFloor(player, state)) {
            state.jumpEscapeConsumed(false);
        }

        // ── Enforce virtual floor (safety net) ──────
        double floorY = state.floorY();
        if (player.getY() < floorY && yVelAfterTravel <= 0.0D) {
            player.setPos(player.getX(), floorY, player.getZ());
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            player.setOnGround(true);
        } else if (Math.abs(player.getY() - floorY) < FLOOR_EPSILON && yVelAfterTravel <= 0.0D) {
            player.setOnGround(true);
        }

        // ── Prevent fall damage ─────────────────────
        player.resetFallDistance();

        // ── Sync floorY to client ───────────────────
        syncState(player, state);
    }

    // ════════════════════════════════════════════════
    //  Terrain & Collision Helpers
    // ════════════════════════════════════════════════

    private static boolean isOnVirtualFloor(ServerPlayer player, KamuiIntangibilityState state) {
        return Math.abs(player.getY() - state.floorY()) < FLOOR_EPSILON;
    }

    /**
     * Scans downward from {@code fromY} to find the highest solid block
     * whose top surface can serve as a floor. Returns the Y of that
     * top surface (blockY + 1.0 for full blocks). If no support is found
     * within {@link #MAX_SUPPORT_SCAN_DEPTH} blocks, returns {@code fromY}.
     */
    private static double findSupportBelow(ServerPlayer player, double fromY) {
        Level level = player.level();
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) - 1; // the block directly below feet

        int minY = startY - MAX_SUPPORT_SCAN_DEPTH;

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.getCollisionShape(level, pos).isEmpty()) {
                return y + 1.0D;
            }
        }

        // No support found — keep current floorY to prevent void fall
        return fromY;
    }

    private static boolean hasSolidCollision(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return !blockState.getCollisionShape(level, pos).isEmpty();
    }

    // ════════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════════

    private static void clearNearbyMobAggro(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS),
                mob -> mob.getTarget() == player
        )) {
            mob.setTarget(null);
        }
    }

    private static void syncState(ServerPlayer player, KamuiIntangibilityState state) {
        long gameTime = player.level().getGameTime();

        PacketDistributor.sendToPlayer(
                player,
                new KamuiIntangibilityStatePayload(
                        state.isActive(),
                        state.floorY(),
                        state.remainingActiveSeconds(gameTime)
                )
        );
    }

    // ════════════════════════════════════════════════
    //  Defense Events
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isProtected(player.level().getGameTime())) {
            event.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isProtected(player.level().getGameTime())) {
            event.setCanceled(true);
            event.getProjectile().discard();
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
            if (state.isActive()) {
                state.makeVulnerable(player.level().getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
            deactivate(player, state, player.level().getGameTime(), true);
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isProtected(player.level().getGameTime())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof ServerPlayer player)) {
                return false;
            }

            KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
            return state.isProtected(player.level().getGameTime());
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE).clearAfterDeath();
        player.noPhysics = player.isSpectator();
        if (!player.isSpectator()) {
            player.setNoGravity(false);
        }
    }
}