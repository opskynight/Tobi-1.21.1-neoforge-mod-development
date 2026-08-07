package com.tobi.tobimod.common.waypoints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * One saved Kamui navigation destination.
 *
 * <p>Per the master plan a waypoint stores exactly a custom name, X/Y/Z and a
 * dimension ID. Yaw/pitch is deliberately not stored: arrival facing is
 * whatever the player is already facing. The Self Kamui origin is a separate
 * record that does keep facing.
 *
 * <p>An "empty" slot is represented by {@link #EMPTY} rather than by null so
 * the waypoint list is always exactly {@link KamuiWaypoints#MAX_WAYPOINTS}
 * entries long and slot indices stay stable across saves and deletes.
 */
public record KamuiWaypoint(String name, double x, double y, double z, ResourceKey<Level> dimension) {
    /** Longest accepted waypoint name. Keeps labels readable inside a wheel slice. */
    public static final int MAX_NAME_LENGTH = 15;

    /** Placeholder occupying an unused slot. */
    public static final KamuiWaypoint EMPTY = new KamuiWaypoint("", 0.0D, 0.0D, 0.0D, Level.OVERWORLD);

    public static final Codec<KamuiWaypoint> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(KamuiWaypoint::name),
                    Codec.DOUBLE.fieldOf("x").forGetter(KamuiWaypoint::x),
                    Codec.DOUBLE.fieldOf("y").forGetter(KamuiWaypoint::y),
                    Codec.DOUBLE.fieldOf("z").forGetter(KamuiWaypoint::z),
                    Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(KamuiWaypoint::dimension)
            ).apply(instance, KamuiWaypoint::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, KamuiWaypoint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            KamuiWaypoint::name,
            ByteBufCodecs.DOUBLE,
            KamuiWaypoint::x,
            ByteBufCodecs.DOUBLE,
            KamuiWaypoint::y,
            ByteBufCodecs.DOUBLE,
            KamuiWaypoint::z,
            ResourceKey.streamCodec(Registries.DIMENSION),
            KamuiWaypoint::dimension,
            KamuiWaypoint::new
    );

    /** A slot counts as used once it has a non-blank name. */
    public boolean isEmpty() {
        return name.isBlank();
    }

    /** Short dimension label for GUI display, for example {@code overworld}. */
    public String dimensionLabel() {
        return dimension.location().getPath();
    }

    /** Rounded coordinate label for GUI display, for example {@code (120, 64, -310)}. */
    public String coordinateLabel() {
        return String.format("(%d, %d, %d)", (int) x, (int) y, (int) z);
    }

    /** Copy of this waypoint carrying a new name, with the name clamped to the allowed length. */
    public KamuiWaypoint withName(String newName) {
        return new KamuiWaypoint(sanitizeName(newName), x, y, z, dimension);
    }

    /** Trims a name and clamps it to {@link #MAX_NAME_LENGTH}. Never returns null. */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }

        String trimmed = raw.trim();
        return trimmed.length() <= MAX_NAME_LENGTH ? trimmed : trimmed.substring(0, MAX_NAME_LENGTH);
    }
}