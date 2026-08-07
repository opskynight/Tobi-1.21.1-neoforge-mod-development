package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet: jump key state while underground.
 * Sent when jump is pressed or released during underground mode.
 * Server uses this to apply smooth 0.15/tick upward movement
 * and rounding on release.
 */
public record KamuiVerticalMovePayload(boolean jumpHeld) implements CustomPacketPayload {

    public static final Type<KamuiVerticalMovePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_vertical_move"));

    public static final StreamCodec<FriendlyByteBuf, KamuiVerticalMovePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    KamuiVerticalMovePayload::jumpHeld,
                    KamuiVerticalMovePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}