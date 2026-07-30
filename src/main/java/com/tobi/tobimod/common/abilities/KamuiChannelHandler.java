package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.world.KamuiTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side channel system for Kamui travel abilities.
 *
 * <p>When the player triggers an enter/leave/waypoint/manual teleport, the
 * action is deferred by 60 ticks (3 seconds). During the channel:
 * <ul>
 *   <li>Kamui intangibility is forcibly disabled (player becomes vulnerable).</li>
 *   <li>If the player takes any damage, the channel is cancelled.</li>
 *   <li>On completion, the teleport fires.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiChannelHandler {
    public static final int CHANNEL_TICKS = 60; // 3 seconds

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

    private record ChannelData(Action action, int waypointSlot, double x, double y, double z, long startTime) {
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
        ACTIVE_CHANNELS.put(player.getUUID(), new ChannelData(Action.ENTER_KAMUI, -1, 0, 0, 0, now));
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
    }

    /** Starts a 3-second channel to leave the Kamui void dimension. */
    public static void startLeaveChannel(ServerPlayer player) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ACTIVE_CHANNELS.put(player.getUUID(), new ChannelData(Action.LEAVE_KAMUI, -1, 0, 0, 0, now));
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
    }

    /** Starts a 3-second channel to travel to a saved waypoint. */
    public static void startTravelToWaypoint(ServerPlayer player, int slot) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ACTIVE_CHANNELS.put(player.getUUID(), new ChannelData(Action.TRAVEL_TO_WAYPOINT, slot, 0, 0, 0, now));
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
    }

    /** Starts a 3-second channel to travel to manual coordinates (same dimension). */
    public static void startTravelToCoords(ServerPlayer player, double x, double y, double z) {
        long now = player.level().getGameTime();
        deactivateKamui(player);
        ACTIVE_CHANNELS.put(player.getUUID(), new ChannelData(Action.TRAVEL_TO_COORDS, -1, x, y, z, now));
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"), true);
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

    /** Cancels an active channel and notifies the player. */
    private static void cancelChannel(ServerPlayer player, Component reason) {
        UUID uuid = player.getUUID();
        if (ACTIVE_CHANNELS.remove(uuid) != null) {
            player.displayClientMessage(reason, true);
        }
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
    //  Event handlers
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
        int remaining = data.remainingTicks(now);

        if (remaining <= 0) {
            // Channel complete — teleport.
            ACTIVE_CHANNELS.remove(uuid);
            executeChannel(player, data);
        } else if (remaining == CHANNEL_TICKS - 1 || remaining % 20 == 0) {
            // Show progress every second. Display +1 so 20 ticks → 1s, 40 → 2s, etc.
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
            ACTIVE_CHANNELS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (ACTIVE_CHANNELS.containsKey(player.getUUID())) {
            cancelChannel(player, Component.translatable("message.tobimod.kamui_channel_interrupted"));
        }
    }
}
