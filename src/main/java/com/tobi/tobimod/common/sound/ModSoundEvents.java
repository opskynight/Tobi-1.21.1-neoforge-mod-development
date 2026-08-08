package com.tobi.tobimod.common.sound;

import com.tobi.tobimod.TobiMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * All custom sound events for the mod.
 *
 * <p>Registered through the NeoForge deferred register system so they are
 * available on both sides. Actual playback is client-only; the server just
 * references the registry object when telling clients to play a sound.
 */
public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TobiMod.MOD_ID);

    /** Universal Kamui channel sound. Plays while any Kamui action is active. */
    public static final Supplier<SoundEvent> KAMUI_CHANNEL = SOUND_EVENTS.register(
            "kamui_channel",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_channel")
            )
    );

    /** Phased dodge sound — ONE event randomly picks phased1 or phased2 (all blocked hits while intangible). */
    public static final Supplier<SoundEvent> KAMUI_PHASED = SOUND_EVENTS.register(
            "kamui_phased",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "kamui_phased")
            )
    );

    private ModSoundEvents() {}
}
