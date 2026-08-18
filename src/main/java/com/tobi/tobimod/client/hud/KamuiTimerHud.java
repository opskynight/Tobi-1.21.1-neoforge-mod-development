package com.tobi.tobimod.client.hud;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Small, non-interactive timer centered below the normal crosshair. */
@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class KamuiTimerHud {
    private static final int TEXT_COLOR = 0xD8A0FFFF;
    private static final int BACKGROUND_COLOR = 0x90000000;

    private KamuiTimerHud() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!KamuiIntangibilityStatePayload.shouldRenderTimer()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics graphics = event.getGuiGraphics();

        String text = "KAMUI • " + KamuiIntangibilityStatePayload.getDisplayedSeconds() + "s";
        int textWidth = font.width(text);

        int x = (graphics.guiWidth() - textWidth) / 2;
        int y = graphics.guiHeight() / 2 + 12;

        graphics.fill(x - 4, y - 3, x + textWidth + 4, y + font.lineHeight + 3, BACKGROUND_COLOR);
        graphics.drawString(font, text, x, y, TEXT_COLOR, false);
    }
}