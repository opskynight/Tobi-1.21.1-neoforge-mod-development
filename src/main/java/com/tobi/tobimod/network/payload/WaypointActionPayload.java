package com.tobi.tobimod.network.payload;

import com.tobi.tobimod.TobiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server waypoint management request.
 *
 * <p>The client never sends coordinates. For {@link Action#SAVE} the server
 * reads the sender's own position, so a modified client cannot save a waypoint
 * at a place the player has not stood.
 */
public record WaypointActionPayload(Action action, int slot, String name) implements CustomPacketPayload {
    public enum Action {
        /** Store the sender's current position in the slot under the given name. */
        SAVE,
        /** Rename an existing waypoint without moving it. */
        RENAME,
        /** Clear the slot. */
        DELETE
    }

    public static final Type<WaypointActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "waypoint_action"));

    private static final Action[] ACTIONS = Action.values();

    /**
     * Sends the action as a plain byte. An out-of-range value from a malformed
     * client falls back to {@link Action#DELETE} on its own slot rather than
     * throwing while decoding.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, Action> ACTION_STREAM_CODEC =
            StreamCodec.of(
                    (buffer, action) -> buffer.writeByte(action.ordinal()),
                    buffer -> {
                        int id = buffer.readByte();
                        return id >= 0 && id < ACTIONS.length ? ACTIONS[id] : Action.DELETE;
                    }
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ACTION_STREAM_CODEC,
                    WaypointActionPayload::action,
                    ByteBufCodecs.VAR_INT,
                    WaypointActionPayload::slot,
                    ByteBufCodecs.STRING_UTF8,
                    WaypointActionPayload::name,
                    WaypointActionPayload::new
            );

    public static WaypointActionPayload save(int slot, String name) {
        return new WaypointActionPayload(Action.SAVE, slot, name);
    }

    public static WaypointActionPayload rename(int slot, String name) {
        return new WaypointActionPayload(Action.RENAME, slot, name);
    }

    public static WaypointActionPayload delete(int slot) {
        return new WaypointActionPayload(Action.DELETE, slot, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}