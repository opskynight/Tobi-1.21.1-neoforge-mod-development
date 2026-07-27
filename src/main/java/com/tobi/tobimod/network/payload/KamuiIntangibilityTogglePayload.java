package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A zero-data request sent when the player presses the Kamui keybind. */
public record KamuiIntangibilityTogglePayload() implements CustomPacketPayload {
    public static final Type<KamuiIntangibilityTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    TobiMod.MOD_ID,
                    "kamui_intangibility_toggle"
            ));

    public static final StreamCodec<FriendlyByteBuf, KamuiIntangibilityTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new KamuiIntangibilityTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}