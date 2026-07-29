package com.tobi.tobimod.common.waypoints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fixed-size waypoint book attached to a player.
 *
 * <p>The list is always exactly {@link #MAX_WAYPOINTS} entries long. Unused
 * slots hold {@link KamuiWaypoint#EMPTY} rather than null so a delete never
 * shifts the remaining waypoints into different wheel positions.
 *
 * <p>Instances are immutable; every mutation returns a new object. The server
 * stores this as a player Attachment and is the only authority over it.
 */
public record KamuiWaypoints(List<KamuiWaypoint> slots) {
    /** Maximum saved favourites per player. */
    public static final int MAX_WAYPOINTS = 10;

    public static final Codec<KamuiWaypoints> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    KamuiWaypoint.CODEC.listOf().fieldOf("slots").forGetter(KamuiWaypoints::slots)
            ).apply(instance, KamuiWaypoints::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, KamuiWaypoints> STREAM_CODEC = StreamCodec.composite(
            KamuiWaypoint.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_WAYPOINTS)),
            KamuiWaypoints::slots,
            KamuiWaypoints::new
    );

    /** Normalises any incoming list to exactly {@link #MAX_WAYPOINTS} entries. */
    public KamuiWaypoints(List<KamuiWaypoint> slots) {
        List<KamuiWaypoint> normalised = new ArrayList<>(MAX_WAYPOINTS);

        if (slots != null) {
            for (int i = 0; i < Math.min(slots.size(), MAX_WAYPOINTS); i++) {
                KamuiWaypoint waypoint = slots.get(i);
                normalised.add(waypoint == null ? KamuiWaypoint.EMPTY : waypoint);
            }
        }

        while (normalised.size() < MAX_WAYPOINTS) {
            normalised.add(KamuiWaypoint.EMPTY);
        }

        this.slots = Collections.unmodifiableList(normalised);
    }

    /** A book with every slot empty. Used as the Attachment default. */
    public KamuiWaypoints() {
        this(List.of());
    }

    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < MAX_WAYPOINTS;
    }

    /** Returns the waypoint in a slot, or {@link KamuiWaypoint#EMPTY} if the index is out of range. */
    public KamuiWaypoint get(int slot) {
        return isValidSlot(slot) ? slots.get(slot) : KamuiWaypoint.EMPTY;
    }

    /** Replaces one slot. Out-of-range indices are ignored rather than throwing. */
    public KamuiWaypoints with(int slot, KamuiWaypoint waypoint) {
        if (!isValidSlot(slot)) {
            return this;
        }

        List<KamuiWaypoint> copy = new ArrayList<>(slots);
        copy.set(slot, waypoint == null ? KamuiWaypoint.EMPTY : waypoint);
        return new KamuiWaypoints(copy);
    }

    /** Clears one slot back to empty. */
    public KamuiWaypoints without(int slot) {
        return with(slot, KamuiWaypoint.EMPTY);
    }
}