package com.tobi.tobimod.client.hud;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class KamuiScoutHud {
    private static final int TEXT_COLOR = 0xA0FFA0FF;
    private static final int BACKGROUND_COLOR = 0x90000000;

    private KamuiScoutHud() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!KamuiScoutStatePayload.isClientActive()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();

        float speed = KamuiScoutStatePayload.getClientFlySpeed();
        // speed is 0.05 default, multiply by 20 for display like spectator? Use formatted
        String text = "KAMUI SCOUT • " + String.format("%.2f", speed) + "  [Scroll: speed | Middle: reset]";
        int w = font.width(text);
        int x = (graphics.guiWidth() - w) / 2;
        int y = graphics.guiHeight() / 2 + 28;
        graphics.fill(x - 4, y - 3, x + w + 4, y + font.lineHeight + 3, BACKGROUND_COLOR);
        graphics.drawString(font, text, x, y, TEXT_COLOR, false);
    }
}
