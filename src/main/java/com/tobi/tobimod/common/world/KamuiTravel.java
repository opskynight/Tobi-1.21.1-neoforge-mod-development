package com.tobi.tobimod.common.world;

import com.tobi.tobimod.TobiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Shared, server-authoritative travel rules for the Kamui pocket dimension. */
public final class KamuiTravel {
    private static final String ORIGIN = TobiMod.MOD_ID + ".kamui_origin";
    private static final int MAX_SCAN_Y = 383;

    private KamuiTravel() {}

    /** Teleports to the fixed X/Z origin, standing safely above whatever column exists there. */
    public static void enter(ServerPlayer player) {
        ServerLevel kamui = player.server.getLevel(TobiMod.KAMUI_DIMENSION);
        if (kamui == null) return;

        var tag = player.getPersistentData().getCompound(ORIGIN);
        tag.putString("dimension", player.level().dimension().location().toString());
        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yaw", player.getYRot());
        tag.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(ORIGIN, tag);
        teleport(player, kamui, 0.5D, safeY(kamui), 0.5D);
    }

    /** Returns to the saved origin; overworld spawn is the safe fallback if it is unavailable. */
    public static void leave(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(ORIGIN);
        ServerLevel destination = player.server.overworld();
        double x = destination.getSharedSpawnPos().getX() + .5D;
        double y = destination.getSharedSpawnPos().getY();
        double z = destination.getSharedSpawnPos().getZ() + .5D;
        if (tag.contains("dimension")) {
            try {
                var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(tag.getString("dimension")));
                if (player.server.getLevel(key) != null) {
                    destination = player.server.getLevel(key);
                    x = tag.getDouble("x"); y = tag.getDouble("y"); z = tag.getDouble("z");
                }
            } catch (Exception ignored) { }
        }
        teleport(player, destination, x, y, z);
        player.getPersistentData().remove(ORIGIN);
    }

    private static int safeY(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, MAX_SCAN_Y, 0);
        for (int y = MAX_SCAN_Y; y >= level.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return y + 1;
        }
        return 64; // defensive fallback if the void has not generated at the origin yet
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        player.teleportTo(level, x, y, z, java.util.Set.of(), player.getYRot(), player.getXRot());
    }
}
