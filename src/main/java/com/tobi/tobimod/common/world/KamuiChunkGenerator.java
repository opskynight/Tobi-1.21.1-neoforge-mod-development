package com.tobi.tobimod.common.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chunk generator for the Kamui void dimension.
 *
 * <p><b>Layout:</b>
 * <pre>
 * Y Y Y Y Y Y Y Y Y
 * Y X X X X X X X Y
 * Y X X X X X X X Y
 * Y X X X X X X X Y
 * Y X X X 0 X X X Y
 * Y X X X X X X X Y
 * Y X X X X X X X Y
 * Y X X X X X X X Y
 * Y Y Y Y Y Y Y Y Y
 * </pre>
 *
 * <p><b>X (chunks -3..+3):</b> Flat solid floor at y=64 — battle arena.
 * <br><b>Y (1-chunk border ring):</b> Solid wall ring, no gaps, height 69–79.
 * <br><b>Beyond Y:</b> Isolated rectangular pillars (8–12 block footprints)
 * with void gaps between them, height 75–105 (30-level variation).
 */
public class KamuiChunkGenerator extends ChunkGenerator {

    // ── Central flat arena ────────────────────────────────
    private static final int ARENA_CHUNK_MIN = -3;
    private static final int ARENA_CHUNK_MAX = 3;
    private static final int ARENA_Y = 64;

    // ── Border ring ───────────────────────────────────────
    private static final int RING_CHUNK_MIN = -4;
    private static final int RING_CHUNK_MAX = 4;
    private static final int RING_MIN_Y = 69;
    private static final int RING_MAX_Y = 79;
    private static final int RING_RANGE = RING_MAX_Y - RING_MIN_Y;

    // ── Outer pillars ─────────────────────────────────────
    private static final int GRID_SIZE = 16;
    private static final int PILLAR_MIN_Y = 75;
    private static final int PILLAR_MAX_Y = 105;
    private static final int PILLAR_RANGE = PILLAR_MAX_Y - PILLAR_MIN_Y;
    /** ~70% of grid cells contain a pillar. */
    private static final int DENSITY_THRESHOLD = 7;

    public static final MapCodec<KamuiChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(KamuiChunkGenerator::getBiomeSource)
            ).apply(instance, KamuiChunkGenerator::new)
    );

    public KamuiChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ── Zone checks ──────────────────────────────────────

    private static boolean isInArena(ChunkPos pos) {
        return pos.x >= ARENA_CHUNK_MIN && pos.x <= ARENA_CHUNK_MAX
                && pos.z >= ARENA_CHUNK_MIN && pos.z <= ARENA_CHUNK_MAX;
    }

    private static boolean isInRing(ChunkPos pos) {
        return pos.x >= RING_CHUNK_MIN && pos.x <= RING_CHUNK_MAX
                && pos.z >= RING_CHUNK_MIN && pos.z <= RING_CHUNK_MAX
                && !isInArena(pos);
    }

    // ── Ring height (per 48-block cell) ────────────────────

    private static int ringHeightAt(int x, int z) {
        int cellX = Math.floorDiv(x, 48);
        int cellZ = Math.floorDiv(z, 48);
        return RING_MIN_Y + Math.floorMod(cellX * 31 + cellZ * 17, RING_RANGE);
    }

    // ── Outer pillar helpers ──────────────────────────────

    private static int cellOf(int coord) {
        return Math.floorDiv(coord, GRID_SIZE);
    }

    private static boolean hasPillar(int cellX, int cellZ) {
        return Math.floorMod(cellX * 13 + cellZ * 37, 10) < DENSITY_THRESHOLD;
    }

    private static int pillarHeight(int cellX, int cellZ) {
        return PILLAR_MIN_Y + Math.floorMod(cellX * 31 + cellZ * 17, PILLAR_RANGE);
    }

    private static int pillarWidthX(int cellX, int cellZ) {
        return 8 + Math.floorMod(cellX * 7 + cellZ * 13, 5);
    }

    private static int pillarWidthZ(int cellX, int cellZ) {
        return 8 + Math.floorMod(cellX * 11 + cellZ * 5, 5);
    }

    private static boolean isInPillar(int x, int z, int cellX, int cellZ) {
        if (!hasPillar(cellX, cellZ)) return false;

        int cellOriginX = cellX * GRID_SIZE;
        int cellOriginZ = cellZ * GRID_SIZE;

        int widthX = pillarWidthX(cellX, cellZ);
        int widthZ = pillarWidthZ(cellX, cellZ);

        int startX = cellOriginX + (GRID_SIZE - widthX) / 2;
        int startZ = cellOriginZ + (GRID_SIZE - widthZ) / 2;

        return x >= startX && x < startX + widthX
                && z >= startZ && z < startZ + widthZ;
    }

    // ──────────────────────────────────────────────
    //  Terrain generation
    // ──────────────────────────────────────────────

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        if (isInArena(chunkPos)) {
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = 0; y <= ARENA_Y; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), false);
                    }
                }
            }
        } else if (isInRing(chunkPos)) {
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    int topY = ringHeightAt(x, z);
                    for (int y = 0; y <= topY; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), false);
                    }
                }
            }
        } else {
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    int cellX = cellOf(x);
                    int cellZ = cellOf(z);

                    if (!isInPillar(x, z, cellX, cellZ)) {
                        continue;
                    }

                    int topY = pillarHeight(cellX, cellZ);
                    for (int y = 0; y <= topY; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region,
                             StructureManager structureManager,
                             RandomState randomState,
                             ChunkAccess chunk) {}

    @Override
    public void applyCarvers(WorldGenRegion region,
                             long seed,
                             RandomState randomState,
                             BiomeManager biomeManager,
                             StructureManager structureManager,
                             ChunkAccess chunk,
                             GenerationStep.Carving carving) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public void addDebugScreenInfo(List<String> info,
                                   RandomState randomState,
                                   BlockPos pos) {}

    @Override
    public int getMinY() { return 0; }

    @Override
    public int getGenDepth() { return 256; }

    @Override
    public int getSeaLevel() { return 64; }

    @Override
    public int getBaseHeight(int x, int z,
                             Heightmap.Types type,
                             LevelHeightAccessor level,
                             RandomState randomState) {
        ChunkPos cp = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (isInArena(cp)) return ARENA_Y;
        if (isInRing(cp)) return ringHeightAt(x, z);
        int cellX = cellOf(x);
        int cellZ = cellOf(z);
        if (isInPillar(x, z, cellX, cellZ)) return pillarHeight(cellX, cellZ);
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z,
                                     LevelHeightAccessor level,
                                     RandomState randomState) {
        BlockState[] states = new BlockState[level.getHeight()];
        ChunkPos cp = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));

        int topY;
        if (isInArena(cp)) {
            topY = ARENA_Y;
        } else if (isInRing(cp)) {
            topY = ringHeightAt(x, z);
        } else {
            int cellX = cellOf(x);
            int cellZ = cellOf(z);
            if (isInPillar(x, z, cellX, cellZ)) {
                topY = pillarHeight(cellX, cellZ);
            } else {
                return new NoiseColumn(level.getMinBuildHeight(), states);
            }
        }

        int minY = level.getMinBuildHeight();
        for (int y = minY; y <= topY; y++) {
            states[y - minY] = Blocks.GRAY_CONCRETE.defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }
}