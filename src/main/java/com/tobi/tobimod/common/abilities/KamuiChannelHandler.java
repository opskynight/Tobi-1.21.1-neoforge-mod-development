package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.world.KamuiTravel;
import com.tobi.tobimod.network.payload.KamuiChannelSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side 3-second channel system for ALL Kamui travel abilities.
 *
 * <p>When the player triggers enter/leave/waypoint/manual teleport, the action is
 * deferred by 60 ticks (3 seconds). During the channel:
 * <ul>
 *   <li>Kamui intangibility is forcibly disabled (player becomes vulnerable).</li>
 *   <li>kamui_channel.ogg (3s) plays on the client — interrupted → sound stops + channel cancels.</li>
 *   <li>Any damage, any movement beyond 0.3 blocks, or pressing R cancels the channel (and stops sound).</li>
 *   <li>While channeling, attack / block break / block use / item use are blocked (you can't do anything).</li>
 *   <li>Pressing R while channeling cancels the channel and force-activates intangibility (bypasses cooldown).</li>
 *   <li>On completion, the teleport fires and the sound has already finished naturally.</li>
 * </ul>
 * All travel types (dimension + waypoint + manual coords) share the same sound & interrupt logic.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiChannelHandler {
    public static final int CHANNEL_TICKS = 60; // 3 seconds
    private static final double MOVEMENT_CANCEL_SQR = 0.09; // 0.3 blocks

    private static final Map<UUID, ChannelData> ACTIVE_CHANNELS = new HashMap<>();

    private KamuiChannelHandler() {}

    // ──────────────────────────────────────────────
    //  Channel action types
    // ──────────────────────────────────────────────

    public enum Action {
        ENTER_KAMUI,
        LEAVE_KAMUI,
        TRAVEL_TO_WAYPOINT,
        TRAVEL_TO_COORDS
    }

    private record ChannelData(
            Action action,
            int waypointSlot,
            double x, double y, double z, // for TRAVEL_TO_COORDS destination
            long startTime,
            double startX, double startY, double startZ // where channel started — for movement cancel
    ) {
        boolean isComplete(long now) {
            return now - startTime >= CHANNEL_TICKS;
        }

        int remainingTicks(long now) {
            return (int) (CHANNEL_TICKS - (now - startTime));
        }
    }

    // ──────────────────────────────────────────────
    //  Public API — called by handlers
    // ──────────────────────────────────────────────

    /** Starts a 3-second channel to enter the Kamui void dimension. */
    public static void startEnterChannel(ServerPlayer player) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ChannelData data = new ChannelData(Action.ENTER_KAMUI, -1, 0, 0, 0, now,
                player.getX(), player.getY(), player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(), data);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
        PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }

    /** Starts a 3-second channel to leave the Kamui void dimension. */
    public static void startLeaveChannel(ServerPlayer player) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ChannelData data = new ChannelData(Action.LEAVE_KAMUI, -1, 0, 0, 0, now,
                player.getX(), player.getY(), player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(), data);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
        PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }

    /** Starts a 3-second channel to travel to a saved waypoint. */
    public static void startTravelToWaypoint(ServerPlayer player, int slot) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ChannelData data = new ChannelData(Action.TRAVEL_TO_WAYPOINT, slot, 0, 0, 0, now,
                player.getX(), player.getY(), player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(), data);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
        PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }

    /** Starts a 3-second channel to travel to manual coordinates (same dimension). */
    public static void startTravelToCoords(ServerPlayer player, double x, double y, double z) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ChannelData data = new ChannelData(Action.TRAVEL_TO_COORDS, -1, x, y, z, now,
                player.getX(), player.getY(), player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(), data);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
        PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }

    /** Returns true if the given player is currently channeling. */
    public static boolean isChanneling(Player player) {
        return ACTIVE_CHANNELS.containsKey(player.getUUID());
    }

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    /** Forces Kamui intangibility off during channel. */
    private static void deactivateKamui(ServerPlayer player) {
        KamuiIntangibilityState state = player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if (state.isActive()) {
            KamuiIntangibilityHandler.deactivate(player, state, player.level().getGameTime(), true);
        }
    }

    /** Cancels an active channel, notifies player and tells client to stop sound. */
    private static void cancelChannel(ServerPlayer player, Component reason) {
        UUID uuid = player.getUUID();
        if (ACTIVE_CHANNELS.remove(uuid) != null) {
            player.displayClientMessage(reason, true);
            PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));
        }
    }

    /** Cancels without chat message (used for R-cancel where we immediately activate kamui). */
    private static void cancelChannelSilent(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (ACTIVE_CHANNELS.remove(uuid) != null) {
            PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));
        }
    }

    /** Called from client movement-cancel payload. */
    public static void handleClientCancel(ServerPlayer player) {
        if (isChanneling(player)) {
            cancelChannel(player, Component.translatable("message.tobimod.kamui_channel_interrupted"));
        }
    }

    /**
     * Called from KamuiIntangibilityHandler when R is pressed while channeling.
     * Cancels channel and force-activates intangibility bypassing cooldown.
     * @return true if was channeling and now cancelled+activated
     */
    public static boolean cancelChannelAndActivateKamui(ServerPlayer player) {
        if (!isChanneling(player)) return false;
        cancelChannelSilent(player);
        // Force activate intangibility bypassing cooldown
        KamuiIntangibilityHandler.forceActivate(player);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_interrupted"), true);
        return true;
    }

    /** Executes the completed channel teleport. */
    private static void executeChannel(ServerPlayer player, ChannelData data) {
        switch (data.action) {
            case ENTER_KAMUI -> KamuiTravel.enter(player);
            case LEAVE_KAMUI -> KamuiTravel.leave(player);
            case TRAVEL_TO_WAYPOINT -> KamuiTravel.travelToWaypoint(player, data.waypointSlot);
            case TRAVEL_TO_COORDS -> KamuiTravel.travelToCoords(player, data.x, data.y, data.z);
        }
    }

    // ──────────────────────────────────────────────
    //  Event handlers — tick & interrupts
    // ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        ChannelData data = ACTIVE_CHANNELS.get(uuid);
        if (data == null) {
            return;
        }

        long now = player.level().getGameTime();

        // ── Movement check — any movement beyond threshold cancels ──
        double dx = player.getX() - data.startX;
        double dy = player.getY() - data.startY;
        double dz = player.getZ() - data.startZ;
        double distSqr = dx * dx + dy * dy + dz * dz;
        if (distSqr > MOVEMENT_CANCEL_SQR) {
            cancelChannel(player, Component.translatable("message.tobimod.kamui_channel_interrupted"));
            return;
        }

        int remaining = data.remainingTicks(now);

        if (remaining <= 0) {
            // Channel complete — teleport. Sound already finished naturally (non-looping 3s).
            ACTIVE_CHANNELS.remove(uuid);
            executeChannel(player, data);
        } else if (remaining == CHANNEL_TICKS - 1 || remaining % 20 == 0) {
            int seconds = (remaining + 19) / 20;
            player.displayClientMessage(
                    Component.translatable("message.tobimod.kamui_channel_progress", seconds),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Silent removal — client is gone
            ACTIVE_CHANNELS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (ACTIVE_CHANNELS.containsKey(player.getUUID())) {
            cancelChannel(player, Component.translatable("message.tobimod.kamui_channel_interrupted"));
        }
    }

    // ──────────────────────────────────────────────
    //  Block actions while channeling (you can't do anything)
    // ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isChanneling(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Silent cancel — player died, channel stops and sound should stop
            if (ACTIVE_CHANNELS.remove(player.getUUID()) != null) {
                PacketDistributor.sendToPlayer(player, new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));
            }
        }
    }
}
