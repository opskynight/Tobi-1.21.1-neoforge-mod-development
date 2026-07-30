package com.tobi.tobimod.common.world;

import com.tobi.tobimod.TobiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Builds Kamui's deliberately sparse, repeating concrete-column landscape. */
// Disabled while dimension bootstrapping is verified; block placement during ChunkEvent.Load can re-enter world generation.
public final class KamuiVoidColumns {
    private KamuiVoidColumns() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().location().equals(TobiMod.KAMUI_DIMENSION_LOCATION)) {
            return;
        }

        var chunk = event.getChunk();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                // One broad, uneven pillar every 24 blocks; the gaps remain true void.
                if (Math.floorMod(x, 24) < 3 && Math.floorMod(z, 24) < 3) {
                    int height = 12 + Math.floorMod(x * 31 + z * 17, 30);
                    for (int y = 0; y <= height; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}