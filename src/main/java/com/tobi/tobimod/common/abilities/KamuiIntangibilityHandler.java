package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
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
 * <p>Vertical navigation:
 * <ul>
 *   <li>Look up + tap jump inside wall → <b>Phase Ascend</b>: teleport to surface.</li>
 *   <li>Hold jump → <b>Step-Up</b>: +1 every 4 ticks with solid-below check.</li>
 *   <li>Hold shift → floorY sinks: instant on first press, then every 5 ticks.</li>
 *   <li>Walk off a ledge → auto-adjust drops floorY, player falls naturally.</li>
 * </ul>
 *
 * <h3)Floor chase</h3>
 * <p>When the player is above floorY inside a solid block (vanilla jump moved them
 * up through noPhysics), the server raises floorY to match. This ensures the floor
 * follows the player upward through walls so they don't fall back when releasing jump.
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

    private static final Map<UUID, Long> LAST_JUMP_RAISE_TICK = new HashMap<>();
    private static final Map<UUID, Boolean> PREV_SHIFT_DOWN = new HashMap<>();

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

                        syncState(player, state);
                        return;
                    }
                }

                // ── +1 raise (Step-Up or Phase Ascend in air) ──
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

                syncState(player, state);
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
        syncState(player, state);
    }

    static void deactivate(Player player, KamuiIntangibilityState state, long now, boolean manual) {
        if (!state.isActive()) {
            return;
        }

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

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
            syncState(serverPlayer, state);
        }
    }

    private static void applyKamuiMode(ServerPlayer player) {
        player.noPhysics = true;

        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    // ════════════════════════════════════════════════
    //  Virtual Floor — Pre-Tick
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

        player.noPhysics = true;

        if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
            player.setPos(player.getX(), floorY, player.getZ());
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            player.setOnGround(true);
            player.setNoGravity(true);
        }
    }

    // ════════════════════════════════════════════════
    //  Virtual Floor — Post-Tick
    // ════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) {
            UUID uuid = player.getUUID();
            LAST_JUMP_RAISE_TICK.remove(uuid);
            PREV_SHIFT_DOWN.remove(uuid);
            return;
        }

        long now = player.level().getGameTime();
        if (state.hasExpired(now)) {
            deactivate(player, state, now, false);
            return;
        }

        UUID uuid = player.getUUID();

        player.setNoGravity(false);
        player.noPhysics = true;

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
                // Just pressed shift → instant sink 1 block
                double newFloor = state.floorY() - 1.0D;
                double newSupported = findSupportBelow(player, newFloor);
                state.floorY(newSupported);
                // Snap player down to new floor immediately
                player.setPos(player.getX(), newSupported, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
                state.sinkAccumulator(0);
            } else {
                // Holding shift → continuous sink
                int acc = state.sinkAccumulator() + 1;
                if (acc >= SINK_TICKS_PER_BLOCK) {
                    acc = 0;
                    double newFloor = state.floorY() - 1.0D;
                    double newSupported = findSupportBelow(player, newFloor);
                    state.floorY(newSupported);
                    // Snap player down to new floor immediately
                    player.setPos(player.getX(), newSupported, player.getZ());
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                    player.setOnGround(true);
                }
                state.sinkAccumulator(acc);
            }
        } else {
            state.sinkAccumulator(0);
        }

        // ── Floor chase: raise floorY when player is above it inside a wall ──
        // Only runs when NOT holding shift (shift-sink and floor chase fight
        // each other — floor chase would undo shift-sink by raising floorY
        // back up to the player's old position).
        if (!shiftDown) {
            currentFloor = state.floorY(); // re-read after auto-adjust
            if (player.getY() > currentFloor + 0.5D) {
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
        syncState(player, state);
    }

    // ════════════════════════════════════════════════
    //  Terrain & Collision Helpers
    // ════════════════════════════════════════════════

    private static boolean isOnVirtualFloor(ServerPlayer player, KamuiIntangibilityState state) {
        return Math.abs(player.getY() - state.floorY()) < FLOOR_EPSILON;
    }

    private static boolean isFeetInsideSolid(ServerPlayer player) {
        Level level = player.level();
        BlockPos feetPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        return hasSolidCollision(level, feetPos);
    }

    private static double findSurfaceAbove(ServerPlayer player, double fromY) {
        Level level = player.level();
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) + 1;

        for (int y = startY; y < startY + MAX_SUPPORT_SCAN_DEPTH; y++) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            BlockState blockState = level.getBlockState(pos);

            if (blockState.getCollisionShape(level, pos).isEmpty()) {
                BlockPos belowPos = pos.below();
                BlockState belowState = level.getBlockState(belowPos);
                if (!belowState.getCollisionShape(level, belowPos).isEmpty()) {
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
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.getCollisionShape(level, pos).isEmpty()) {
                return y + 1.0D;
            }
        }

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
    // ═══════════════-════════════════════════════════

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