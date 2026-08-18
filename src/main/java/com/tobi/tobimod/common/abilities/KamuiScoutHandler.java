package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiScoutSpeedPayload;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiScoutHandler {
    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_scout_flight");
    private static final double AGGRO_CLEAR_RADIUS = 64.0D;

    private KamuiScoutHandler() {}

    // ═══════════════════════════ Payload handlers ═══════════════════════════

    public static void handleScoutAction(com.tobi.tobimod.network.payload.KamuiScoutActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                var action = payload.action();
                if (action == com.tobi.tobimod.network.payload.KamuiScoutActionPayload.Action.ENTER) {
                    // Start channel to enter scout at current position
                    KamuiChannelHandler.startScoutEnterChannel(sp);
                } else if (action == com.tobi.tobimod.network.payload.KamuiScoutActionPayload.Action.EXIT) {
                    KamuiChannelHandler.startScoutExitChannel(sp);
                }
            }
        });
    }

    public static void handleSpeedPayload(KamuiScoutSpeedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                KamuiScoutState state = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
                if (!state.isActive()) return;
                float clamped = Math.clamp(payload.speed(), KamuiScoutState.MIN_SCOUT_SPEED, KamuiScoutState.MAX_SCOUT_SPEED);
                state.scoutSpeed(clamped);
                sp.getAbilities().setFlyingSpeed(clamped);
                sp.onUpdateAbilities();
                // sync back so client and other logic stay consistent
                PacketDistributor.sendToPlayer(sp, new KamuiScoutStatePayload(true, clamped));
            }
        });
    }

    // ═══════════════════════════ Lifecycle ═══════════════════════════

    public static boolean tryActivate(ServerPlayer player) {
        long now = player.level().getGameTime();
        KamuiScoutState scout = player.getData(TobiMod.KAMUI_SCOUT_STATE);
        KamuiIntangibilityState kamui = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);

        if (scout.isActive()) return false;
        if (scout.isOnCooldown(now)) {
            // optional: send cooldown message
            return false;
        }
        // mutually exclusive with intangibility — deactivate kamui if active
        if (kamui.isActive()) {
            KamuiIntangibilityHandler.deactivate(player, kamui, now, true);
        }
        // cancel travel channel if somehow active? channel handler will handle
        scout.activate(player, now);
        applyScoutMode(player, scout);
        clearNearbyMobAggro(player);
        forceSync(player, scout);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_scout_enter"), true);
        return true;
    }

    public static boolean tryDeactivate(ServerPlayer player, boolean manual) {
        KamuiScoutState state = player.getData(TobiMod.KAMUI_SCOUT_STATE);
        if (!state.isActive()) return false;
        deactivate(player, state, player.level().getGameTime(), manual);
        player.displayClientMessage(Component.translatable(manual ? "message.tobimod.kamui_scout_exit" : "message.tobimod.kamui_scout_exit"), true);
        return true;
    }

    static void deactivate(Player player, KamuiScoutState state, long now, boolean manual) {
        if (!state.isActive()) return;

        player.noPhysics = state.originalNoPhysics();
        player.setNoGravity(state.originalNoGravity());
        player.removeEffect(MobEffects.NIGHT_VISION);

        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
        }

        player.getAbilities().flying = state.originalFlying();
        player.getAbilities().setFlyingSpeed(state.originalFlyingSpeed());
        // ensure mayfly via attribute? The attribute modifier remains removed; vanilla mayfly based on gamemode will restore after onUpdateAbilities
        if (player instanceof ServerPlayer sp) {
            sp.setInvisible(state.originalInvisible());
        } else {
            player.setInvisible(state.originalInvisible());
        }
        player.setPose(Pose.STANDING);

        if (player.isPassenger()) {
            // keep riding? spectator auto-dismounts; we mimic that on entry only
        }

        state.deactivate(now, manual);

        if (player instanceof ServerPlayer sp) {
            sp.onUpdateAbilities();
            // Find safe ground if inside block after exit — nudge up
            ensureNotInsideSolid(sp);
            forceSync(sp, state);
        }
    }

    private static void ensureNotInsideSolid(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        BlockState st = level.getBlockState(pos);
        BlockState st2 = level.getBlockState(pos.above());
        if (!st.getCollisionShape(level, pos).isEmpty() || !st2.getCollisionShape(level, pos.above()).isEmpty()) {
            // try to find surface above
            int blockX = pos.getX();
            int blockZ = pos.getZ();
            int startY = pos.getY();
            for (int y = startY; y < startY + 32; y++) {
                BlockPos p = new BlockPos(blockX, y, blockZ);
                BlockPos above = p.above();
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                        && level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                    player.setPos(player.getX(), y, player.getZ());
                    break;
                }
            }
        }
    }

    private static void applyScoutMode(ServerPlayer player, KamuiScoutState state) {
        player.noPhysics = true;
        player.setNoGravity(true);
        // night vision for xray visibility underground
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 999999, 0, false, false, false));

        // Enable creative flight via NeoForge attribute (non-creative)
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) {
            flightAttribute.removeModifier(FLIGHT_MODIFIER_ID);
            // attribute expects addOrUpdate? Use transient modifier with value 1. But easier: directly enable via abilities mayfly + set flying
            // However per NeoForge docs: value 1 with ADD_VALUE enables flight. We'll mimic IntangibilityHandler which just clears modifier and sets flying false.
            // For scout we DO want flight enabled even in survival.
            // So we add modifier 1
            flightAttribute.addOrUpdateTransientModifier(
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            FLIGHT_MODIFIER_ID, 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(state.scoutSpeed());
        player.setInvisible(true);
        // Force standing
        player.setPose(Pose.STANDING);
        if (player.isPassenger()) player.stopRiding();
        player.onUpdateAbilities();
    }

    private static void forceSync(ServerPlayer player, KamuiScoutState state) {
        PacketDistributor.sendToPlayer(player, new KamuiScoutStatePayload(state.isActive(), state.scoutSpeed()));
    }

    private static void clearNearbyMobAggro(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS),
                mob -> mob.getTarget() == player
        )) {
            mob.setTarget(null);
        }
    }

    // ═══════════════════════════ Ticks ═══════════════════════════

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KamuiScoutState state = player.getData(TobiMod.KAMUI_SCOUT_STATE);
        if (!state.isActive()) return;

        player.noPhysics = true;
        player.setNoGravity(true);
        player.resetFallDistance();
        // keep invisible and flying
        player.setInvisible(true);
        player.getAbilities().flying = true;
        // pose suppression
        if (player.getPose() == Pose.SWIMMING || player.getPose() == Pose.CROUCHING
                || player.getPose() == Pose.FALL_FLYING || player.getPose() == Pose.SLEEPING) {
            player.setPose(Pose.STANDING);
        }
        if (player.isPassenger()) player.stopRiding();
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KamuiScoutState state = player.getData(TobiMod.KAMUI_SCOUT_STATE);
        if (!state.isActive()) return;

        // ensure flight attribute still present (in case something removed)
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null && !flightAttribute.hasModifier(FLIGHT_MODIFIER_ID)) {
            flightAttribute.addOrUpdateTransientModifier(
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            FLIGHT_MODIFIER_ID, 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    )
            );
            player.getAbilities().flying = true;
        }

        // ensure speed synced
        if (player.getAbilities().getFlyingSpeed() != state.scoutSpeed()) {
            player.getAbilities().setFlyingSpeed(state.scoutSpeed());
            player.onUpdateAbilities();
        }

        player.noPhysics = true;
        player.setNoGravity(true);
        player.resetFallDistance();
        player.setInvisible(true);
        // refresh night vision if needed
        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 999999, 0, false, false, false));
        }

        Pose p = player.getPose();
        if (p == Pose.SWIMMING || p == Pose.CROUCHING || p == Pose.FALL_FLYING || p == Pose.SLEEPING) {
            player.setPose(Pose.STANDING);
        }
        if (player.isPassenger()) player.stopRiding();
    }

    // ═══════════════════════════ Defense / interaction blocking ═══════════════════════════

    @SubscribeEvent
    public static void onInvulnCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setInvulnerable(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult ehr
                && ehr.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) {
                event.setCanceled(true);
                event.getProjectile().discard();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) {
                event.setCanceled(true);
                // also null out just in case event is not cancellable on some versions
                event.setNewAboutToBeSetTarget(null);
                if (event.getEntity() instanceof Mob mob && mob.getTarget() == sp) {
                    mob.setTarget(null);
                }
            }
        }
        // Also handle case where Mob already targeting scout via setTarget directly
        if (event.getEntity() instanceof Mob mob) {
            var tgt = event.getNewAboutToBeSetTarget();
            if (tgt instanceof ServerPlayer sp) {
                KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
                if (s.isActive()) event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(e -> {
            if (e instanceof ServerPlayer sp) {
                KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
                return s.isActive();
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    // Block all interactions while scouting (spectator parity)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemOnBlock(net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            if (s.isActive()) {
                deactivate(sp, s, sp.level().getGameTime(), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            PacketDistributor.sendToPlayer(sp, new KamuiScoutStatePayload(s.isActive(), s.isActive() ? s.scoutSpeed() : KamuiScoutState.DEFAULT_SCOUT_SPEED));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            // scout cleared on death via onPlayerClone, but ensure client synced
            PacketDistributor.sendToPlayer(sp, new KamuiScoutStatePayload(s.isActive(), KamuiScoutState.DEFAULT_SCOUT_SPEED));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KamuiScoutState s = sp.getData(TobiMod.KAMUI_SCOUT_STATE);
            PacketDistributor.sendToPlayer(sp, new KamuiScoutStatePayload(s.isActive(), s.isActive() ? s.scoutSpeed() : KamuiScoutState.DEFAULT_SCOUT_SPEED));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player p = event.getEntity();
        var state = p.getData(TobiMod.KAMUI_SCOUT_STATE);
        state.clearAfterDeath();
        p.noPhysics = p.isSpectator();
        if (!p.isSpectator()) p.setNoGravity(false);
        if (p instanceof ServerPlayer sp) {
            // force sync inactive
            PacketDistributor.sendToPlayer(sp, new KamuiScoutStatePayload(false, KamuiScoutState.DEFAULT_SCOUT_SPEED));
        }
    }

    // Also ensure EnderMan anger etc via LivingChangeTarget already covered
}
