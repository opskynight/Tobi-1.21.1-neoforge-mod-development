package com.tobi.tobimod.client.screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.common.waypoints.KamuiWaypoint;
import com.tobi.tobimod.common.waypoints.KamuiWaypoints;
import com.tobi.tobimod.network.payload.WaypointActionPayload;
import com.tobi.tobimod.network.payload.WaypointSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Names a new waypoint or renames an existing one.
 *
 * <p>Coordinates are shown read-only. When creating, the server records the
 * player's own position, so the preview here is informational only.
 */
public class WaypointEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 104;
    private static final int COLOR_TITLE = 0xFFD8A0FF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_PANEL = 0xC0000000;

    private final int slot;
    private final boolean creating;

    private EditBox nameField;
    private int ticksOpened;

    public WaypointEditorScreen(int slot, boolean creating) {
        super(Component.translatable(creating
                ? "screen.tobimod.save_waypoint"
                : "screen.tobimod.rename_waypoint"));
        this.slot = slot;
        this.creating = creating;
    }

    private KamuiWaypoint waypoint() {
        return WaypointSyncPayload.clientWaypoints().get(slot);
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

        this.nameField = new EditBox(
                this.font,
                left + 12,
                top + 34,
                PANEL_WIDTH - 24,
                this.font.lineHeight + 6,
                Component.translatable("screen.tobimod.waypoint_name")
        );
        this.nameField.setMaxLength(KamuiWaypoint.MAX_NAME_LENGTH);
        this.nameField.setValue(defaultName());
        this.nameField.moveCursorToEnd(false);
        addRenderableWidget(this.nameField);

        addRenderableWidget(new ExtendedButton(
                left + 12,
                top + PANEL_HEIGHT - 28,
                100,
                18,
                Component.translatable("screen.tobimod.confirm"),
                button -> confirm()
        ));

        addRenderableWidget(new ExtendedButton(
                left + PANEL_WIDTH - 112,
                top + PANEL_HEIGHT - 28,
                100,
                18,
                Component.translatable("screen.tobimod.cancel"),
                button -> onClose()
        ));
    }

    private String defaultName() {
        KamuiWaypoint existing = waypoint();
        if (!existing.isEmpty()) {
            return existing.name();
        }

        return "Waypoint " + (slot + 1);
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(this.nameField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        guiGraphics.drawString(this.font, this.title, left + 12, top + 10, COLOR_TITLE, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.tobimod.waypoint_name"),
                left + 12,
                top + 24,
                COLOR_LABEL,
                false
        );

        String position = creating
                ? Component.translatable("screen.tobimod.current_position").getString()
                : waypoint().dimensionLabel() + " " + waypoint().coordinateLabel();
        guiGraphics.drawString(this.font, position, left + 12, top + 60, COLOR_LABEL, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void confirm() {
        String name = KamuiWaypoint.sanitizeName(this.nameField.getValue());
        if (name.isBlank() || !KamuiWaypoints.isValidSlot(slot)) {
            onClose();
            return;
        }

        PacketDistributor.sendToServer(creating
                ? WaypointActionPayload.save(slot, name)
                : WaypointActionPayload.rename(slot, name));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            confirm();
            return true;
        }

        // Ignore the opening keybind briefly so releasing it cannot close this screen.
        if (ticksOpened < 20 && keyCode == ModKeybindings.KAMUI_NAVIGATION.getKey().getValue()) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Stop the opening key's own character landing in the name box.
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