package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.waypoints.KamuiWaypoints;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client copy of the player's waypoint book, used purely so the
 * navigation GUI has something to draw.
 *
 * <p>Sent on login and after any successful change: never on a tick loop. The
 * client copy is display-only and is not trusted by the server.
 */
public record WaypointSyncPayload(KamuiWaypoints waypoints) implements CustomPacketPayload {
    private static KamuiWaypoints clientWaypoints = new KamuiWaypoints();

    public static final Type<WaypointSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "waypoint_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    KamuiWaypoints.STREAM_CODEC,
                    WaypointSyncPayload::waypoints,
                    WaypointSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Last known waypoint book for the local player. Client display only. */
    public static KamuiWaypoints clientWaypoints() {
        return clientWaypoints;
    }

    /** Clears the cached copy, for example on disconnect. */
    public static void resetClientWaypoints() {
        clientWaypoints = new KamuiWaypoints();
    }

    public static void handle(WaypointSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientWaypoints = payload.waypoints());
    }
}