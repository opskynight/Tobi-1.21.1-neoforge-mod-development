package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client sync for Kamui Scout.
 */
public record KamuiScoutStatePayload(boolean active, float flySpeed) implements CustomPacketPayload {
    private static boolean clientActive = false;
    private static float clientFlySpeed = 0.05F;

    public static final Type<KamuiScoutStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_scout_state"));

    public static final StreamCodec<FriendlyByteBuf, KamuiScoutStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    KamuiScoutStatePayload::active,
                    ByteBufCodecs.FLOAT,
                    KamuiScoutStatePayload::flySpeed,
                    KamuiScoutStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static boolean isClientActive() { return clientActive; }
    public static float getClientFlySpeed() { return clientFlySpeed; }

    public static void setClientFlySpeed(float speed) { clientFlySpeed = speed; }

    public static void resetClient() {
        clientActive = false;
        clientFlySpeed = 0.05F;
    }

    public static void handle(KamuiScoutStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientActive = payload.active();
            clientFlySpeed = payload.active() ? payload.flySpeed() : 0.05F;

            Player player = context.player();
            if (player != null) {
                if (!payload.active() && !player.isSpectator()) {
                    player.noPhysics = false;
                    player.setNoGravity(false);
                    // don't force invisible false here — server will sync via data, but clear client prediction
                    player.setInvisible(false);
                    // restore flying speed default, abilities will be overwritten by server on next tick
                    player.getAbilities().setFlyingSpeed(0.05F);
                } else if (payload.active()) {
                    player.noPhysics = true;
                    player.setNoGravity(true);
                    player.setInvisible(true);
                    player.getAbilities().setFlyingSpeed(payload.flySpeed());
                    player.getAbilities().flying = true;
                }
            }
        });
    }
}
