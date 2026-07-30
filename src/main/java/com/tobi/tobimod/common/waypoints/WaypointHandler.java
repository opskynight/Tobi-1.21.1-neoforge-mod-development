package com.tobi.tobimod.common.waypoints;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiChannelHandler;
import com.tobi.tobimod.network.payload.WaypointActionPayload;
import com.tobi.tobimod.network.payload.WaypointSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side authority for the waypoint book.
 *
 * <p>All mutations happen here. The client only ever asks; it never supplies
 * coordinates. Every accepted change is followed by a single sync packet, and
 * nothing in this class runs on a tick loop.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class WaypointHandler {
    private WaypointHandler() {}

    public static void handleActionPayload(WaypointActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                apply(player, payload);
            }
        });
    }

    private static void apply(ServerPlayer player, WaypointActionPayload payload) {
if (payload.action() == WaypointActionPayload.Action.ENTER_KAMUI) { KamuiChannelHandler.startEnterChannel(player); return; }
        if (payload.action() == WaypointActionPayload.Action.LEAVE_KAMUI) { KamuiChannelHandler.startLeaveChannel(player); return; }
        if (payload.action() == WaypointActionPayload.Action.TRAVEL) { KamuiChannelHandler.startTravelToWaypoint(player, payload.slot()); return; }
        int slot = payload.slot();
        if (!KamuiWaypoints.isValidSlot(slot)) {
            return;
        }

        KamuiWaypoints book = player.getData(TobiMod.KAMUI_WAYPOINTS);
        KamuiWaypoints updated = switch (payload.action()) {
            case SAVE -> save(player, book, slot, payload.name());
            case RENAME -> rename(book, slot, payload.name());
            case DELETE -> book.without(slot);
            case ENTER_KAMUI, LEAVE_KAMUI, TRAVEL -> book;
        };

        if (updated == book) {
            return;
        }

        player.setData(TobiMod.KAMUI_WAYPOINTS, updated);
        sync(player, updated);
    }

    private static KamuiWaypoints save(ServerPlayer player, KamuiWaypoints book, int slot, String requestedName) {
        String name = KamuiWaypoint.sanitizeName(requestedName);
        if (name.isBlank()) {
            name = "Waypoint " + (slot + 1);
        }

        KamuiWaypoint waypoint = new KamuiWaypoint(
                name,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.level().dimension()
        );

        return book.with(slot, waypoint);
    }

    /** Renames an existing waypoint. Empty slots and blank names are rejected. */
    private static KamuiWaypoints rename(KamuiWaypoints book, int slot, String requestedName) {
        KamuiWaypoint existing = book.get(slot);
        if (existing.isEmpty()) {
            return book;
        }

        String name = KamuiWaypoint.sanitizeName(requestedName);
        if (name.isBlank() || name.equals(existing.name())) {
            return book;
        }

        return book.with(slot, existing.withName(name));
    }

    public static void sync(ServerPlayer player, KamuiWaypoints book) {
        PacketDistributor.sendToPlayer(player, new WaypointSyncPayload(book));
    }

    public static void sync(ServerPlayer player) {
        sync(player, player.getData(TobiMod.KAMUI_WAYPOINTS));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** Waypoints survive death, so re-send them after a respawn or dimension change. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}