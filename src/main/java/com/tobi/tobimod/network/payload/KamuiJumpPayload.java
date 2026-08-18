package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet for Phase Ascend and Step-Up.
 *
 * <ul>
 *   <li>{@code stepUp=false} → Phase Ascend (tap + look up): teleport to surface or +1 raise.</li>
 *   <li>{@code stepUp=true} → Step-Up (hold): +1 raise with solid check.</li>
 * </ul>
 */
public record KamuiJumpPayload(boolean stepUp) implements CustomPacketPayload {

    public static final Type<KamuiJumpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_jump"));

    public static final StreamCodec<FriendlyByteBuf, KamuiJumpPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    KamuiJumpPayload::stepUp,
                    KamuiJumpPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}