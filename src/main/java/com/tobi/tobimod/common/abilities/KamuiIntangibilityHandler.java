package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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



@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiIntangibilityHandler {
    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_temporary_flight");
    private static final double ENTRY_STEP = 0.15D;
    private static final double WALL_CHECK_DISTANCE = 0.45D;
    private static final double AGGRO_CLEAR_RADIUS = 32.0D;
    private static final float UNDERGROUND_FLYING_SPEED = 0.10F;

    private KamuiIntangibilityHandler() {}

    public static void handleTogglePayload(KamuiIntangibilityTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                toggle(player);
            }
        });
    }

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
        applySurfaceMode(player);
        syncState(player, state);
    }

    private static void deactivate(Player player, KamuiIntangibilityState state, long now, boolean manual) {
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

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
            syncState(serverPlayer, state);
        }
    }

    private static void applySurfaceMode(ServerPlayer player) {
        player.noPhysics = false;
        player.setNoGravity(false);

        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = false;
        player.setDeltaMovement(Vec3.ZERO);
        player.onUpdateAbilities();
    }

    private static void applyUndergroundMode(ServerPlayer player) {
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null && !flightAttribute.hasModifier(FLIGHT_MODIFIER_ID)) {
            flightAttribute.addTransientModifier(
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            FLIGHT_MODIFIER_ID,
                            1.0D,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        player.noPhysics = true;
        player.setNoGravity(true);
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(UNDERGROUND_FLYING_SPEED);
        player.setDeltaMovement(Vec3.ZERO);
        player.onUpdateAbilities();
        clearNearbyPlayerAggro(player);
    }

    private static void syncState(ServerPlayer player, KamuiIntangibilityState state) {
        long gameTime = player.level().getGameTime();

        PacketDistributor.sendToPlayer(
                player,
                new KamuiIntangibilityStatePayload(
                        state.isActive(),
                        state.isUnderground(),
                        state.remainingActiveSeconds(gameTime)
                )
        );
    }

    private static boolean hasSolidCollision(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return !blockState.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isHeadInsideSolidBlock(Player player) {
        BlockPos headPos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return hasSolidCollision(player.level(), headPos);
    }

    /** Three clear blocks at feet/head height plus a solid supporting block below. */
    /**
     * Surface exit check.
     *
     * The player needs three clear collision spaces. A supporting block is NOT
     * required directly below them: if Kamui exits slightly above the surface,
     * normal gravity safely brings them down instead of leaving underground
     * flight enabled forever.
     */
    private static boolean hasValidSurfaceSpace(Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        Level level = player.level();

        return !hasSolidCollision(level, feet)
                && !hasSolidCollision(level, feet.above())
                && !hasSolidCollision(level, feet.above(2));
    }

    /**
     * Intentional horizontal terrain entry. Only runs while Shift is held and a
     * solid collision block is directly in the player's horizontal look direction.
     */
    private static boolean tryEnterWall(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            return false;
        }
        horizontal = horizontal.normalize();

        BlockPos bodyAhead = BlockPos.containing(
                player.getX() + horizontal.x * WALL_CHECK_DISTANCE,
                player.getY() + 1.0D,
                player.getZ() + horizontal.z * WALL_CHECK_DISTANCE
        );

        if (!hasSolidCollision(player.level(), bodyAhead)) {
            return false;
        }

        player.teleportTo(
                player.getX() + horizontal.x * ENTRY_STEP,
                player.getY(),
                player.getZ() + horizontal.z * ENTRY_STEP
        );
        return true;
    }

    /** Intentional floor entry, matching the datapack's controlled Y teleport. */
    private static void sinkIntoFloor(ServerPlayer player) {
        player.teleportTo(player.getX(), player.getY() - ENTRY_STEP, player.getZ());
    }

    /** Runs only once when entering underground mode. */
    private static void clearNearbyPlayerAggro(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS),
                mob -> mob.getTarget() == player
        )) {
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (!state.isActive()) {
            return;
        }

        long now = player.level().getGameTime();
        if (state.hasExpired(now)) {
            deactivate(player, state, now, false);
            return;
        }

        if (!state.isUnderground() && player.isShiftKeyDown()) {
            // Prefer horizontal wall entry if a wall is directly ahead. Otherwise
            // use the datapack-style vertical floor sink.
            if (!tryEnterWall(player)) {
                sinkIntoFloor(player);
            }
        }

        boolean changed = state.updateMovementMode(
                isHeadInsideSolidBlock(player),
                hasValidSurfaceSpace(player)
        );

        if (changed) {
            if (state.isUnderground()) {
                applyUndergroundMode(player);
                syncState(player, state);
            } else {
                applySurfaceMode(player);
                syncState(player, state);
            }
        }
    }

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