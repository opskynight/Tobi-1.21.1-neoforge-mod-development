package com.tobi.tobimod.network;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityHandler;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
import com.tobi.tobimod.network.payload.KamuiVerticalMovePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.tobi.tobimod.common.waypoints.WaypointHandler;
import com.tobi.tobimod.network.payload.KamuiChannelCancelPayload;
import com.tobi.tobimod.network.payload.KamuiChannelSyncPayload;
import com.tobi.tobimod.network.payload.KamuiScoutActionPayload;
import com.tobi.tobimod.network.payload.KamuiScoutSpeedPayload;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import com.tobi.tobimod.network.payload.ManualTeleportPayload;
import com.tobi.tobimod.network.payload.WaypointActionPayload;
import com.tobi.tobimod.network.payload.WaypointSyncPayload;

@EventBusSubscriber(modid = TobiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PacketHandler {
    private PacketHandler() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(WaypointActionPayload.TYPE, WaypointActionPayload.STREAM_CODEC, WaypointHandler::handleActionPayload);
        r.playToClient(WaypointSyncPayload.TYPE, WaypointSyncPayload.STREAM_CODEC, WaypointSyncPayload::handle);
        r.playToServer(KamuiIntangibilityTogglePayload.TYPE, KamuiIntangibilityTogglePayload.STREAM_CODEC, KamuiIntangibilityHandler::handleTogglePayload);
        r.playToServer(KamuiJumpPayload.TYPE, KamuiJumpPayload.STREAM_CODEC, KamuiIntangibilityHandler::handleJumpPayload);
        r.playToServer(KamuiVerticalMovePayload.TYPE, KamuiVerticalMovePayload.STREAM_CODEC, KamuiIntangibilityHandler::handleVerticalMovePayload);
        r.playToServer(ManualTeleportPayload.TYPE, ManualTeleportPayload.STREAM_CODEC, ManualTeleportPayload::handle);
        r.playToClient(KamuiIntangibilityStatePayload.TYPE, KamuiIntangibilityStatePayload.STREAM_CODEC, KamuiIntangibilityStatePayload::handle);
        r.playToClient(KamuiChannelSyncPayload.TYPE, KamuiChannelSyncPayload.STREAM_CODEC, KamuiChannelSyncPayload::handle);
        r.playToServer(KamuiChannelCancelPayload.TYPE, KamuiChannelCancelPayload.STREAM_CODEC, KamuiChannelCancelPayload::handle);
        r.playToClient(KamuiScoutStatePayload.TYPE, KamuiScoutStatePayload.STREAM_CODEC, KamuiScoutStatePayload::handle);
        r.playToServer(KamuiScoutActionPayload.TYPE, KamuiScoutActionPayload.STREAM_CODEC, KamuiScoutActionPayload::handle);
        r.playToServer(KamuiScoutSpeedPayload.TYPE, KamuiScoutSpeedPayload.STREAM_CODEC, KamuiScoutSpeedPayload::handle);
    }
}