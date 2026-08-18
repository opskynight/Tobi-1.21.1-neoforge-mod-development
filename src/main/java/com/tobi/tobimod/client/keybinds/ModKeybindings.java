package com.tobi.tobimod.client.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.tobi.tobimod.TobiMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = TobiMod.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ModKeybindings {
    private ModKeybindings() {}

    public static final KeyMapping KAMUI_INTANGIBILITY = new KeyMapping(
            "key.tobimod.kamui_intangibility",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.tobimod"
    );

    public static final KeyMapping KAMUI_TRAVEL = new KeyMapping(
            "key.tobimod.kamui_travel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.tobimod"
    );

    /** Opens the Kamui navigation wheel. Never starts a channel directly. */
    public static final KeyMapping KAMUI_NAVIGATION = new KeyMapping(
            "key.tobimod.kamui_navigation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.tobimod"
    );

    public static final KeyMapping BASIC_GENJUTSU = new KeyMapping(
            "key.tobimod.basic_genjutsu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.tobimod"
    );

    public static final KeyMapping ADVANCED_GENJUTSU = new KeyMapping(
            "key.tobimod.advanced_genjutsu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.tobimod"
    );

    public static final KeyMapping KAMUI_WARP = new KeyMapping(
            "key.tobimod.kamui_warp",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.tobimod"
    );

    public static final KeyMapping BLACK_RECEIVERS = new KeyMapping(
            "key.tobimod.black_receivers",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.tobimod"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KAMUI_INTANGIBILITY);
        event.register(KAMUI_TRAVEL);
        event.register(KAMUI_NAVIGATION);
        event.register(BASIC_GENJUTSU);
        event.register(ADVANCED_GENJUTSU);
        event.register(KAMUI_WARP);
        event.register(BLACK_RECEIVERS);
    }
}