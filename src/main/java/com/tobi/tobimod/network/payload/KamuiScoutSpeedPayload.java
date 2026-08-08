package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiScoutHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KamuiScoutSpeedPayload(float speed) implements CustomPacketPayload {
    public static final Type<KamuiScoutSpeedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_scout_speed"));

    public static final StreamCodec<FriendlyByteBuf, KamuiScoutSpeedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    KamuiScoutSpeedPayload::speed,
                    KamuiScoutSpeedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(KamuiScoutSpeedPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        KamuiScoutHandler.handleSpeedPayload(payload, ctx);
    }
}
