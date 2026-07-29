package com.tobi.tobimod.client.screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;

import java.util.function.Predicate;

/**
 * Manual coordinate entry, reached from the wheel's centre button.
 *
 * <p>Transfers are same-dimension only by design. The Teleport button is
 * disabled until the shared three-second vulnerable channel exists; the server
 * will validate build limits, the world border and safe arrival at that point.
 */
public class ManualTeleportScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 116;
    private static final int COLOR_TITLE = 0xFFD8A0FF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_NOTE = 0xFF8A8A8A;
    private static final int COLOR_PANEL = 0xC0000000;

    private final Predicate<String> coordinateValidator = ManualTeleportScreen::isValidCoordinate;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private int ticksOpened;

    public ManualTeleportScreen() {
        super(Component.translatable("screen.tobimod.manual_transfer"));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
    }

    @Override
    public void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int fieldWidth = 62;
        int fieldHeight = this.font.lineHeight + 6;
        int fieldY = top + 38;

        this.xField = createCoordinateField(left + 14, fieldY, fieldWidth, fieldHeight, "X");
        this.yField = createCoordinateField(left + 14 + fieldWidth + 12, fieldY, fieldWidth, fieldHeight, "Y");
        this.zField = createCoordinateField(left + 14 + (fieldWidth + 12) * 2, fieldY, fieldWidth, fieldHeight, "Z");

        prefillWithCurrentPosition();

        ExtendedButton teleportButton = new ExtendedButton(
                left + 14,
                top + PANEL_HEIGHT - 28,
                104,
                18,
                Component.translatable("screen.tobimod.teleport"),
                button -> {}
        );
        // Inert until the shared channel system exists.
        teleportButton.active = false;
        addRenderableWidget(teleportButton);

        addRenderableWidget(new ExtendedButton(
                left + PANEL_WIDTH - 118,
                top + PANEL_HEIGHT - 28,
                104,
                18,
                Component.translatable("screen.tobimod.cancel"),
                button -> onClose()
        ));
    }

    private EditBox createCoordinateField(int x, int y, int width, int height, String label) {
        EditBox field = new EditBox(this.font, x, y, width, height, Component.literal(label));
        field.setMaxLength(12);
        field.setFilter(coordinateValidator);
        addRenderableWidget(field);
        return field;
    }

    private void prefillWithCurrentPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        this.xField.setValue(String.valueOf((int) minecraft.player.getX()));
        this.yField.setValue(String.valueOf((int) minecraft.player.getY()));
        this.zField.setValue(String.valueOf((int) minecraft.player.getZ()));
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(this.xField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        guiGraphics.drawString(this.font, this.title, left + 14, top + 10, COLOR_TITLE, false);

        int labelY = top + 28;
        int fieldWidth = 62;
        guiGraphics.drawString(this.font, "X", left + 14, labelY, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "Y", left + 14 + fieldWidth + 12, labelY, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "Z", left + 14 + (fieldWidth + 12) * 2, labelY, COLOR_LABEL, false);

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.tobimod.same_dimension_only"),
                left + 14,
                top + 66,
                COLOR_NOTE,
                false
        );

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    /** Permits partial input such as "-" or "12." while typing. */
    private static boolean isValidCoordinate(String input) {
        if (input.isEmpty() || input.equals("-") || input.equals(".") || input.equals("-.")) {
            return true;
        }
        if (input.endsWith(".")) {
            return isValidCoordinate(input.substring(0, input.length() - 1));
        }

        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (ticksOpened < 20 && keyCode == ModKeybindings.KAMUI_NAVIGATION.getKey().getValue()) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        String keyName = ModKeybindings.KAMUI_NAVIGATION.getKey().getName();
        if (ticksOpened < 20
                && !keyName.isEmpty()
                && keyName.charAt(keyName.length() - 1) == codePoint) {
            return false;
        }

        return getFocused() != null && getFocused().charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        ticksOpened++;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new KamuiNavigationScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}