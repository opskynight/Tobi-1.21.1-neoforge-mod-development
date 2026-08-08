package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client channel state sync.
 * START -> play 3s kamui_channel sound and enter freeze/lock state.
 * CANCEL -> stop sound and release lock.
 */
public record KamuiChannelSyncPayload(Action action) implements CustomPacketPayload {
    public enum Action {
        START,
        CANCEL
    }

    public static final Type<KamuiChannelSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_channel_sync"));

    private static final Action[] VALUES = Action.values();

    private static final StreamCodec<FriendlyByteBuf, Action> ACTION_CODEC =
            StreamCodec.of(
                    (buf, action) -> buf.writeByte(action.ordinal()),
                    buf -> {
                        int id = buf.readByte();
                        return id >= 0 && id < VALUES.length ? VALUES[id] : Action.CANCEL;
                    }
            );

    public static final StreamCodec<FriendlyByteBuf, KamuiChannelSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ACTION_CODEC,
                    KamuiChannelSyncPayload::action,
                    KamuiChannelSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KamuiChannelSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Dispatch to client handler
            com.tobi.tobimod.client.ClientEventHandler.handleChannelSync(payload);
        });
    }
}
