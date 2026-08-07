package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
import com.tobi.tobimod.network.payload.KamuiVerticalMovePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
 * Server-side handler for Kamui intangibility — dual-mode system.
 *
 * <h3>Underground mode</h3>
 * <p>When the player's body is inside a solid block:
 * <ul>
 *   <li>Hold jump → smooth upward movement at 0.15 blocks/tick</li>
 *   <li>Hold shift → smooth downward movement at 0.15 blocks/tick</li>
 *   <li>Release jump → round Y up to next integer</li>
 *   <li>Release shift → round Y down to integer</li>
 *   <li>Look up + tap jump → Phase Ascend (instant teleport to surface)</li>
 *   <li>Gravity disabled, vanilla jump/sneak suppressed</li>
 * </ul>
 *
 * <h3>Surface mode</h3>
 * <p>When the player's body is NOT inside a solid block:
 * <ul>
 *   <li>Vanilla jump works normally</li>
 *   <li>Hold jump → Step-Up: +1 every 4 ticks with solid-below check</li>
 *   <li>Hold shift → sink through floor (instant first, then every 5 ticks)</li>
 *   <li>Walk into wall → body enters solid → auto-switch to underground</li>
 * </ul>
 *
 * <h3>Pose suppression</h3>
 * <p>While Kamui is active: force Pose.STANDING, prevent swimming/crawling/riding.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiIntangibilityHandler {
    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_temporary_flight");

    private static final int SINK_TICKS_PER_BLOCK = 5;
    private static final int MAX_SUPPORT_SCAN_DEPTH = 128;
    private static final double AGGRO_CLEAR_RADIUS = 32.0D;
    private static final double FLOOR_EPSILON = 0.05D;
    private static final long JUMP_RAISE_COOLDOWN_TICKS = 4;
    /** Smooth movement speed in blocks per tick. */
    private static final double SMOOTH_SPEED = 0.15D;

    // ── Per-player tracking ──
    private static final Map<UUID, Long> LAST_JUMP_RAISE_TICK = new HashMap<>();
    private static final Map<UUID, Boolean> PREV_SHIFT_DOWN = new HashMap<>();
    /** Client-reported jump held state for underground smooth movement. */
    private static final Map<UUID, Boolean> UNDERGROUND_JUMP_HELD = new HashMap<>();
    /** Last synced floorY — only send packet when changed. */
    private static final Map<UUID, Double> LAST_SYNCED_FLOOR_Y = new HashMap<>();
    /** Previous underground state — for mode transition detection. */
    private static final Map<UUID, Boolean> PREV_UNDERGROUND = new HashMap<>();

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

    public static void handleJumpPayload(KamuiJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
                if (!state.isActive()) return;

                UUID uuid = player.getUUID();
                long now = player.level().getGameTime();
                long lastRaise = LAST_JUMP_RAISE_TICK.getOrDefault(uuid, 0L);
                if (now - lastRaise < JUMP_RAISE_COOLDOWN_TICKS) return;

                double currentFloor = state.floorY();

                // ── Phase Ascend: inside wall → teleport to surface ──
                if (!payload.stepUp() && isFeetInsideSolid(player)) {
                    double surfaceY = findSurfaceAbove(player, currentFloor);
                    if (surfaceY > currentFloor) {
                        state.floorY(surfaceY);
                        LAST_JUMP_RAISE_TICK.put(uuid, now);
                        player.setPos(player.getX(), surfaceY, player.getZ());
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                        player.setOnGround(true);
                        forceSyncState(player, state);
                        return;
                    }
                }

                // ── Step-Up: +1 raise with solid check ──
                double newFloor = currentFloor + 1.0D;
                BlockPos belowNewFloor = new BlockPos(
                        Mth.floor(player.getX()),
                        Mth.floor(newFloor) - 1,
                        Mth.floor(player.getZ())
                );
                if (!hasSolidCollision(player.level(), belowNewFloor)) {
                    return;
                }

                state.floorY(newFloor);
                LAST_JUMP_RAISE_TICK.put(uuid, now);
                player.setPos(player.getX(), newFloor, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
                forceSyncState(player, state);
            }
        });
    }

    /** Handles underground jump held/released from client. */
    public static void handleVerticalMovePayload(KamuiVerticalMovePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UUID uuid = player.getUUID();
                boolean wasHeld = UNDERGROUND_JUMP_HELD.getOrDefault(uuid, false);
                boolean isHeld = payload.jumpHeld();
                UNDERGROUND_JUMP_HELD.put(uuid, isHeld);

                KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
                if (!state.isActive()) return;

                // On release while underground → round Y up to integer
                if (wasHeld && !isHeld && isBodyInsideSolid(player)) {
                    double roundedY = Math.ceil(player.getY());
                    if (roundedY == player.getY()) roundedY += 1.0D; // if exactly integer, go up 1
                    player.setPos(player.getX(), roundedY, player.getZ());
                    state.floorY(roundedY);
                    forceSyncState(player, state);
                }
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

        double supportedFloor = findSupportBelow(player, state.floorY());
        state.floorY(supportedFloor);

        applyKamuiMode(player);
        clearNearbyMobAggro(player);
        forceSyncState(player, state);
    }

    static void deactivate(Player player, KamuiIntangibilityState state, long now, boolean manual) {
        if (!state.isActive()) return;

        player.noPhysics = state.originalNoPhysics();
        player.setNoGravity(state.originalNoGravity());

        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = state.originalFlying();
        player.getAbilities().setFlyingSpeed(state.originalFlyingSpeed());
        state.deactivate(now, manual);

        UUID uuid = player.getUUID();
        LAST_JUMP_RAISE_TICK.remove(uuid);
        PREV_SHIFT_DOWN.remove(uuid);
        UNDERGROUND_JUMP_HELD.remove(uuid);
        LAST_SYNCED_FLOOR_Y.remove(uuid);
        PREV_UNDERGROUND.remove(uuid);

        // Restore normal pose
        player.setPose(Pose.STANDING);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
            forceSyncState(serverPlayer, state);
        }
    }

    private static void applyKamuiMode(ServerPlayer player) {
        player.noPhysics = true;

        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = false;

        // Force standing pose (prevents swimming/crawling)
        player.setPose(Pose.STANDING);

        // Dismount any vehicle (boat, horse, minecart, etc.)
        if (player.isPassenger()) {
            player.stopRiding();
        }

        player.onUpdateAbilities();
    }

    // ════════════════════════════════════════════════
    //  Pre-Tick
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) return;

        player.noPhysics = true;

        boolean underground = isBodyInsideSolid(player);

        if (underground) {
            // Underground: disable gravity, suppress vanilla jump, stay at current Y
            player.setNoGravity(true);
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            player.setOnGround(false); // prevent vanilla jumpFromGround
        } else {
            // Surface: jitter prevention (stand on virtual floor)
            double floorY = state.floorY();
            double yVel = player.getDeltaMovement().y;

            if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
                player.setPos(player.getX(), floorY, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
                player.setNoGravity(true);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  Post-Tick
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) {
            UUID uuid = player.getUUID();
            LAST_JUMP_RAISE_TICK.remove(uuid);
            PREV_SHIFT_DOWN.remove(uuid);
            UNDERGROUND_JUMP_HELD.remove(uuid);
            LAST_SYNCED_FLOOR_Y.remove(uuid);
            PREV_UNDERGROUND.remove(uuid);
            return;
        }

        long now = player.level().getGameTime();
        if (state.hasExpired(now)) {
            deactivate(player, state, now, false);
            return;
        }

        UUID uuid = player.getUUID();
        player.noPhysics = true;

        boolean underground = isBodyInsideSolid(player);
        boolean wasUnderground = PREV_UNDERGROUND.getOrDefault(uuid, false);
        PREV_UNDERGROUND.put(uuid, underground);

        // ══════════════════════════════════════════
        //  Underground Mode
        // ══════════════════════════════════════════
        if (underground) {
            player.setNoGravity(true);
            player.resetFallDistance();

            // ── Smooth upward movement (jump held) ──
            boolean jumpHeld = UNDERGROUND_JUMP_HELD.getOrDefault(uuid, false);
            if (jumpHeld) {
                double newY = player.getY() + SMOOTH_SPEED;
                player.setPos(player.getX(), newY, player.getZ());
                state.floorY(newY);
            }

            // ── Smooth downward movement (shift held) ──
            boolean shiftDown = player.isShiftKeyDown();
            boolean wasShiftDown = PREV_SHIFT_DOWN.getOrDefault(uuid, false);
            PREV_SHIFT_DOWN.put(uuid, shiftDown);

            if (shiftDown && !jumpHeld) {
                double newY = player.getY() - SMOOTH_SPEED;
                // Don't fall below support
                double supportedY = findSupportBelow(player, newY);
                newY = Math.max(newY, supportedY);
                player.setPos(player.getX(), newY, player.getZ());
                state.floorY(newY);
            }

            // ── Shift release while underground → round down ──
            if (wasShiftDown && !shiftDown) {
                double roundedY = Math.floor(player.getY());
                player.setPos(player.getX(), roundedY, player.getZ());
                state.floorY(roundedY);
            }

            // Prevent re-mounting vehicles
            if (player.isPassenger()) {
                player.stopRiding();
            }
        }
        // ══════════════════════════════════════════
        //  Surface Mode
        // ══════════════════════════════════════════
        else {
            player.setNoGravity(false);

            // ── Transition from underground → surface ──
            if (wasUnderground) {
                // Just exited a wall/water — find floor and snap
                double currentY = player.getY();
                double floorY = findSupportBelow(player, currentY);
                // Also check water surface
                double waterSurface = getWaterSurfaceY(player, currentY);
                floorY = Math.max(floorY, waterSurface);
                if (currentY < floorY) {
                    player.setPos(player.getX(), floorY, player.getZ());
                }
                state.floorY(Math.max(currentY, floorY));
                player.setOnGround(true);
            }

            // ── Water surface snap: if near water surface, raise floorY to it ──
            {
                double currentY = player.getY();
                double waterSurface = getWaterSurfaceY(player, currentY);
                // If we're within 1 block of a water surface and at or above the floor, snap floorY to water surface
                if (waterSurface > currentY && waterSurface - currentY < 1.0D) {
                    double currentFloor = state.floorY();
                    if (waterSurface > currentFloor) {
                        state.floorY(waterSurface);
                    }
                }
            }

            // ── Auto-adjust: drop floor if standing on air ──
            double currentFloor = state.floorY();
            BlockPos belowFloorPos = new BlockPos(
                    Mth.floor(player.getX()),
                    Mth.floor(currentFloor) - 1,
                    Mth.floor(player.getZ())
            );
            if (!hasSolidCollision(player.level(), belowFloorPos)) {
                double supportedFloor = findSupportBelow(player, currentFloor);
                if (supportedFloor < currentFloor) {
                    state.floorY(supportedFloor);
                }
            }

            // ── Shift: sink with instant first press ──
            boolean shiftDown = player.isShiftKeyDown();
            boolean wasShiftDown = PREV_SHIFT_DOWN.getOrDefault(uuid, false);
            PREV_SHIFT_DOWN.put(uuid, shiftDown);

            if (shiftDown && isOnVirtualFloor(player, state)) {
                if (!wasShiftDown) {
                    double newFloor = state.floorY() - 1.0D;
                    double newSupported = findSupportBelow(player, newFloor);
                    state.floorY(newSupported);
                    player.setPos(player.getX(), newSupported, player.getZ());
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                    player.setOnGround(true);
                    state.sinkAccumulator(0);
                } else {
                    int acc = state.sinkAccumulator() + 1;
                    if (acc >= SINK_TICKS_PER_BLOCK) {
                        acc = 0;
                        double newFloor = state.floorY() - 1.0D;
                        double newSupported = findSupportBelow(player, newFloor);
                        state.floorY(newSupported);
                        player.setPos(player.getX(), newSupported, player.getZ());
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                        player.setOnGround(true);
                    }
                    state.sinkAccumulator(acc);
                }
            } else {
                state.sinkAccumulator(0);
            }

            // ── Floor chase: raise floorY when player above it inside wall ──
            if (!shiftDown) {
                double floorY = state.floorY();
                if (player.getY() > floorY + 0.5D) {
                    BlockPos feetPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
                    if (hasSolidCollision(player.level(), feetPos)) {
                        state.floorY(player.getY());
                    }
                }
            }

            // ── Enforce virtual floor ──
            double floorY = state.floorY();
            double yVel = player.getDeltaMovement().y;
            if (player.getY() < floorY && yVel <= 0.0D) {
                player.setPos(player.getX(), floorY, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
            } else if (Math.abs(player.getY() - floorY) < FLOOR_EPSILON && yVel <= 0.0D) {
                player.setOnGround(true);
            }

            player.resetFallDistance();
        }

        // ══════════════════════════════════════════
        //  Pose Suppression (both modes)
        // ══════════════════════════════════════════
        Pose currentPose = player.getPose();
        if (currentPose == Pose.SWIMMING || currentPose == Pose.CROUCHING ||
                currentPose == Pose.FALL_FLYING || currentPose == Pose.SLEEPING) {
            player.setPose(Pose.STANDING);
        }

        // Prevent re-mounting vehicles
        if (player.isPassenger()) {
            player.stopRiding();
        }

        // ── Conditional sync (only when floorY or mode changes) ──
        syncStateIfNeeded(player, state, underground);
    }

    // ════════════════════════════════════════════════
    //  Terrain & Collision Helpers
    // ════════════════════════════════════════════════

    private static boolean isOnVirtualFloor(ServerPlayer player, KamuiIntangibilityState state) {
        return Math.abs(player.getY() - state.floorY()) < FLOOR_EPSILON;
    }

    /** Body inside solid or water = block at waist level (Y+1) has collision or is water source. */
    private static boolean isBodyInsideSolid(ServerPlayer player) {
        Level level = player.level();
        BlockPos bodyPos = BlockPos.containing(player.getX(), player.getY() + 1.0D, player.getZ());
        return hasSolidCollision(level, bodyPos);
    }

    /** Feet inside solid or water = block at feet level has collision or is water source. */
    private static boolean isFeetInsideSolid(ServerPlayer player) {
        Level level = player.level();
        BlockPos feetPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        return hasSolidCollision(level, feetPos);
    }

    /** Phase Ascend: scan up for first air block with solid/water-source below. Returns standing Y. */
    private static double findSurfaceAbove(ServerPlayer player, double fromY) {
        Level level = player.level();
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) + 1;

        for (int y = startY; y < startY + MAX_SUPPORT_SCAN_DEPTH; y++) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (!hasSolidCollision(level, pos)) {
                // This block is air (no solid, no water source) — check if below is solid/water
                BlockPos belowPos = pos.below();
                if (hasSolidCollision(level, belowPos)) {
                    return y - 1.0D;
                }
            }
        }
        return fromY;
    }

    private static double findSupportBelow(ServerPlayer player, double fromY) {
        Level level = player.level();
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) - 1;
        int minY = startY - MAX_SUPPORT_SCAN_DEPTH;

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (hasSolidCollision(level, pos)) {
                return y + 1.0D;
            }
        }
        return fromY;
    }

    /** Check if block has solid collision shape OR is a water source block. */
    private static boolean hasSolidCollision(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.getCollisionShape(level, pos).isEmpty()) return true;
        // Water source blocks count as solid for Kamui (stand on water surface, climb through water)
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(FluidTags.WATER) && fluidState.isSource();
    }

    /** Check if a position is a water source block. */
    private static boolean isWaterSource(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(FluidTags.WATER) && fluidState.isSource();
    }

    /** Get the Y level of the top of water at player's X/Z.
     *  Returns the Y where a player can stand on the water surface. */
    private static double getWaterSurfaceY(ServerPlayer player, double fromY) {
        Level level = player.level();
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY);
        // Scan up to find the topmost water source block
        int topWaterY = -999;
        for (int y = startY; y < startY + 20; y++) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (isWaterSource(level, pos)) {
                topWaterY = y;
            } else {
                break; // hit non-water above, stop
            }
        }
        if (topWaterY >= startY) {
            return topWaterY + 1.0D; // stand on top of the water source
        }
        return fromY;
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

    /** Always sends state (for activation, deactivation, Phase Ascend). */
    private static void forceSyncState(ServerPlayer player, KamuiIntangibilityState state) {
        boolean underground = isBodyInsideSolid(player);
        long gameTime = player.level().getGameTime();
        PacketDistributor.sendToPlayer(player, new KamuiIntangibilityStatePayload(
                state.isActive(), state.floorY(), state.remainingActiveSeconds(gameTime), underground
        ));
        LAST_SYNCED_FLOOR_Y.put(player.getUUID(), state.floorY());
    }

    /** Only sends when floorY or underground mode changed — saves bandwidth. */
    private static void syncStateIfNeeded(ServerPlayer player, KamuiIntangibilityState state, boolean underground) {
        double currentFloor = state.floorY();
        double lastSynced = LAST_SYNCED_FLOOR_Y.getOrDefault(player.getUUID(), Double.NaN);

        if (Double.isNaN(lastSynced) || Math.abs(currentFloor - lastSynced) > 0.01D) {
            forceSyncState(player, state);
        }
    }

    // ════════════════════════════════════════════════
    //  Defense Events
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isProtected(player.level().getGameTime())) event.setInvulnerable(true);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayer player)) return;
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
            if (state.isActive()) state.makeVulnerable(player.level().getGameTime());
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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isProtected(player.level().getGameTime())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof ServerPlayer player)) return false;
            KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
            return state.isProtected(player.level().getGameTime());
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE).clearAfterDeath();
        player.noPhysics = player.isSpectator();
        if (!player.isSpectator()) player.setNoGravity(false);
    }
}