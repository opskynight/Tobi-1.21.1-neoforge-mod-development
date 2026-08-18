package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server : client detected movement/interrupt and requests channel cancel.
 * Server is authoritative and will cancel if still channeling and echo a CANCEL sync back.
 */
public record KamuiChannelCancelPayload() implements CustomPacketPayload {
    public static final Type<KamuiChannelCancelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_channel_cancel"));

    public static final StreamCodec<FriendlyByteBuf, KamuiChannelCancelPayload> STREAM_CODEC =
            StreamCodec.unit(new KamuiChannelCancelPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KamuiChannelCancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                com.tobi.tobimod.common.abilities.KamuiChannelHandler.handleClientCancel(player);
            }
        });
    }
}