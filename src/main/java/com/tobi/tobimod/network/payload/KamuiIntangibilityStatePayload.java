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
 * Compact server-to-client Kamui state snapshot.
 *
 * <p>Carries the virtual floor Y level and underground mode flag
 * so the client can do accurate prediction.
 */
public record KamuiIntangibilityStatePayload(
        boolean active,
        double floorY,
        int remainingSeconds,
        boolean underground
) implements CustomPacketPayload {
    private static boolean clientKamuiActive;
    private static double clientFloorY;
    private static int clientRemainingSeconds;
    private static int clientTimerTicks;
    private static boolean clientUnderground;

    public static final Type<KamuiIntangibilityStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_intangibility_state"));

    public static final StreamCodec<FriendlyByteBuf, KamuiIntangibilityStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    KamuiIntangibilityStatePayload::active,
                    ByteBufCodecs.DOUBLE,
                    KamuiIntangibilityStatePayload::floorY,
                    ByteBufCodecs.INT,
                    KamuiIntangibilityStatePayload::remainingSeconds,
                    ByteBufCodecs.BOOL,
                    KamuiIntangibilityStatePayload::underground,
                    KamuiIntangibilityStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static boolean isClientKamuiActive() {
        return clientKamuiActive;
    }

    public static double getClientFloorY() {
        return clientFloorY;
    }

    public static boolean isClientUnderground() {
        return clientUnderground;
    }

    public static void predictFloorRaise() {
        if (clientKamuiActive) {
            clientFloorY += 1.0;
        }
    }

    public static void predictFloorSet(double newFloorY) {
        if (clientKamuiActive) {
            clientFloorY = newFloorY;
        }
    }

    public static boolean shouldRenderTimer() {
        return clientKamuiActive && clientRemainingSeconds > 0;
    }

    public static int getDisplayedSeconds() {
        return clientRemainingSeconds;
    }

    public static void tickClientTimer() {
        if (!clientKamuiActive || clientRemainingSeconds <= 0) {
            return;
        }

        clientTimerTicks++;
        if (clientTimerTicks >= 20) {
            clientTimerTicks = 0;
            clientRemainingSeconds--;
        }
    }

    public static void handle(KamuiIntangibilityStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientKamuiActive = payload.active();
            clientFloorY = payload.active() ? payload.floorY() : 0.0;
            clientUnderground = payload.active() && payload.underground();
            clientRemainingSeconds = payload.active() ? Math.max(0, payload.remainingSeconds()) : 0;
            clientTimerTicks = 0;

            Player player = context.player();
            if (player != null && !payload.active() && !player.isSpectator()) {
                player.noPhysics = false;
                player.setNoGravity(false);
            }
        });
    }
}