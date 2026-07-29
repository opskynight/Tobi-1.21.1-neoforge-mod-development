package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.sound.KamuiSoundManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Compact server-to-client Kamui state snapshot.
 * The timer is received only on state changes; the client reduces its displayed
 * seconds locally once per second, with no timer packet spam.
 */
public record KamuiIntangibilityStatePayload(
        boolean active,
        boolean underground,
        int remainingSeconds
) implements CustomPacketPayload {
    private static boolean clientKamuiActive;
    private static boolean clientUnderground;
    private static int clientRemainingSeconds;
    private static int clientTimerTicks;

    public static final Type<KamuiIntangibilityStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_intangibility_state"));

    public static final StreamCodec<FriendlyByteBuf, KamuiIntangibilityStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    KamuiIntangibilityStatePayload::active,
                    ByteBufCodecs.BOOL,
                    KamuiIntangibilityStatePayload::underground,
                    ByteBufCodecs.INT,
                    KamuiIntangibilityStatePayload::remainingSeconds,
                    KamuiIntangibilityStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static boolean isClientUnderground() {
        return clientKamuiActive && clientUnderground;
    }

    public static boolean shouldRenderTimer() {
        return clientKamuiActive && clientRemainingSeconds > 0;
    }

    public static int getDisplayedSeconds() {
        return clientRemainingSeconds;
    }

    /** Called by the client tick handler. The visual count changes only every second. */
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
            // Start or stop the channel sound based on the new state.
            if (payload.active()) {
                KamuiSoundManager.start();
            } else {
                KamuiSoundManager.stop();
            }

            clientKamuiActive = payload.active();
            clientUnderground = payload.underground();
            clientRemainingSeconds = payload.active() ? Math.max(0, payload.remainingSeconds()) : 0;
            clientTimerTicks = 0;

            Player player = context.player();
            if (player != null && !payload.underground() && !player.isSpectator()) {
                player.noPhysics = false;
                player.setNoGravity(false);
            }
        });
    }
}