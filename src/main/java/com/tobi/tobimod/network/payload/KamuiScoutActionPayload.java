package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiScoutHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KamuiScoutActionPayload(Action action) implements CustomPacketPayload {
    public enum Action { ENTER, EXIT }

    public static final Type<KamuiScoutActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_scout_action"));

    private static final Action[] VALUES = Action.values();

    private static final StreamCodec<FriendlyByteBuf, Action> ACTION_CODEC =
            StreamCodec.of(
                    (buf, a) -> buf.writeByte(a.ordinal()),
                    buf -> {
                        int id = buf.readByte();
                        return id >= 0 && id < VALUES.length ? VALUES[id] : Action.ENTER;
                    }
            );

    public static final StreamCodec<FriendlyByteBuf, KamuiScoutActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ACTION_CODEC,
                    KamuiScoutActionPayload::action,
                    KamuiScoutActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(KamuiScoutActionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        KamuiScoutHandler.handleScoutAction(payload, ctx);
    }
}
