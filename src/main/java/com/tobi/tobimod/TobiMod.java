package com.tobi.tobimod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityState;
import com.tobi.tobimod.common.sound.ModSoundEvents;
import com.tobi.tobimod.common.waypoints.KamuiWaypoints;
import com.tobi.tobimod.common.world.KamuiChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(TobiMod.MOD_ID)
public final class TobiMod {
    public static final String MOD_ID = "tobimod";
    public static final ResourceLocation KAMUI_DIMENSION_LOCATION =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "kamui_void");
    public static final ResourceKey<Level> KAMUI_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, KAMUI_DIMENSION_LOCATION);
    public static final Logger LOGGER = LogUtils.getLogger();

    // ── Attachment types ──────────────────────────────

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public static final Supplier<AttachmentType<KamuiIntangibilityState>> KAMUI_INTANGIBILITY_STATE =
            ATTACHMENT_TYPES.register("kamui_intangibility_state", () ->
                    AttachmentType.builder(KamuiIntangibilityState::new)
                            .serialize(KamuiIntangibilityState.CODEC)
                            .build()
            );

    public static final Supplier<AttachmentType<KamuiWaypoints>> KAMUI_WAYPOINTS =
            ATTACHMENT_TYPES.register("kamui_waypoints", () ->
                    AttachmentType.builder(() -> new KamuiWaypoints())
                            .serialize(KamuiWaypoints.CODEC)
                            .copyOnDeath()
                            .build()
            );

    // ── Chunk generator ──────────────────────────────

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> KAMUI_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("kamui", () -> KamuiChunkGenerator.CODEC);

    // ── Init ──────────────────────────────────────────

    public TobiMod(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        ModSoundEvents.SOUND_EVENTS.register(modBus);
        LOGGER.info("Tobi Mod initialized");
    }
}