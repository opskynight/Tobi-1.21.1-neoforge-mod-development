package com.tobi.tobimod.network;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityHandler;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.tobi.tobimod.common.waypoints.WaypointHandler;
import com.tobi.tobimod.network.payload.ManualTeleportPayload;
import com.tobi.tobimod.network.payload.WaypointActionPayload;
import com.tobi.tobimod.network.payload.WaypointSyncPayload;

@EventBusSubscriber(
        modid = TobiMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class PacketHandler {
    private PacketHandler() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                WaypointActionPayload.TYPE,
                WaypointActionPayload.STREAM_CODEC,
                WaypointHandler::handleActionPayload
        );

        registrar.playToClient(
                WaypointSyncPayload.TYPE,
                WaypointSyncPayload.STREAM_CODEC,
                WaypointSyncPayload::handle
        );

        registrar.playToServer(
                KamuiIntangibilityTogglePayload.TYPE,
                KamuiIntangibilityTogglePayload.STREAM_CODEC,
                KamuiIntangibilityHandler::handleTogglePayload
        );

        registrar.playToServer(
                KamuiJumpPayload.TYPE,
                KamuiJumpPayload.STREAM_CODEC,
                KamuiIntangibilityHandler::handleJumpPayload
        );

        registrar.playToServer(
                ManualTeleportPayload.TYPE,
                ManualTeleportPayload.STREAM_CODEC,
                ManualTeleportPayload::handle
        );

        registrar.playToClient(
                KamuiIntangibilityStatePayload.TYPE,
                KamuiIntangibilityStatePayload.STREAM_CODEC,
                KamuiIntangibilityStatePayload::handle
        );
    }
}