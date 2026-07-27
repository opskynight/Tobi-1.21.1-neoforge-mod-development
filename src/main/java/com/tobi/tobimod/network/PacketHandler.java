package com.tobi.tobimod.network;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityHandler;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
                KamuiIntangibilityTogglePayload.TYPE,
                KamuiIntangibilityTogglePayload.STREAM_CODEC,
                KamuiIntangibilityHandler::handleTogglePayload
        );

        registrar.playToClient(
                KamuiIntangibilityStatePayload.TYPE,
                KamuiIntangibilityStatePayload.STREAM_CODEC,
                KamuiIntangibilityStatePayload::handle
        );
    }
}