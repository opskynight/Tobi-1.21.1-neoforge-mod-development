package com.tobi.tobimod;

import com.mojang.logging.LogUtils;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityState;
import com.tobi.tobimod.common.sound.ModSoundEvents;
import com.tobi.tobimod.common.waypoints.KamuiWaypoints;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(TobiMod.MOD_ID)
public final class TobiMod {
    public static final String MOD_ID = "tobimod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    /** The only authoritative status storage for Combat Kamui Intangibility. */
    public static final Supplier<AttachmentType<KamuiIntangibilityState>> KAMUI_INTANGIBILITY_STATE =
            ATTACHMENT_TYPES.register("kamui_intangibility_state", () ->
                    AttachmentType.builder(KamuiIntangibilityState::new)
                            .serialize(KamuiIntangibilityState.CODEC)
                            .build()
            );

    /**
     * Saved Kamui navigation waypoints. Copied on death so dying never costs a
     * player their saved destinations.
     */
    public static final Supplier<AttachmentType<KamuiWaypoints>> KAMUI_WAYPOINTS =
            ATTACHMENT_TYPES.register("kamui_waypoints", () ->
                    AttachmentType.builder(() -> new KamuiWaypoints())
                            .serialize(KamuiWaypoints.CODEC)
                            .copyOnDeath()
                            .build()
            );

    public TobiMod(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
        ModSoundEvents.SOUND_EVENTS.register(modBus);
        LOGGER.info("Tobi Mod initialized");
    }
}