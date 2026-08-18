package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiChannelHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server manual coordinate teleport request.
 *
 * <p>The server starts a 3-second vulnerable channel before executing the
 * teleport. Build limits, world borders and safe arrival are validated
 * server-side.
 */
public record ManualTeleportPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<ManualTeleportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "manual_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManualTeleportPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    ManualTeleportPayload::x,
                    ByteBufCodecs.DOUBLE,
                    ManualTeleportPayload::y,
                    ByteBufCodecs.DOUBLE,
                    ManualTeleportPayload::z,
                    ManualTeleportPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManualTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Clamp to world border and build limits.
                double x = Math.clamp(payload.x, -30000000, 30000000);
                double y = Math.clamp(payload.y, player.level().getMinBuildHeight(),
                        player.level().getMaxBuildHeight());
                double z = Math.clamp(payload.z, -30000000, 30000000);

                KamuiChannelHandler.startTravelToCoords(player, x, y, z);
            }
        });
    }
}
