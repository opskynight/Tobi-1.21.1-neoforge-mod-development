/*
 * The radial slice geometry, mouse-angle maths and open/close behaviour in this
 * class follow the approach used by JustDireThings' AdvPortalRadialMenu, which
 * itself was adapted from code written by Vazkii for the Psi mod.
 * Psi is Open Source and distributed under the Psi License:
 * http://psi.vazkii.us/license.php
 */
package com.tobi.tobimod.client.screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.renderers.TobiRenderTypes;
import com.tobi.tobimod.common.waypoints.KamuiWaypoint;
import com.tobi.tobimod.common.waypoints.KamuiWaypoints;
import com.tobi.tobimod.network.payload.WaypointActionPayload;
import com.tobi.tobimod.network.payload.WaypointSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/**
 * The C navigation wheel.
 *
 * <p>Layout follows the master plan: ten waypoint slices around the outside, a
 * "Choose Coordinates" button in the middle that only navigates, and the
 * context-sensitive Enter Kamui / Return to Origin action as a side button so
 * the ring stays a clean ten slots.
 *
 * <p>This screen performs no teleporting. Teleport actions need the shared
 * three-second vulnerable channel, which does not exist yet, so those controls
 * are present but disabled.
 */
public class KamuiNavigationScreen extends Screen {
    private static final int SEGMENTS = KamuiWaypoints.MAX_WAYPOINTS;
    private static final int RADIUS_MIN = 40;
    private static final int RADIUS_MAX = 120;

    /** Slice growth animation, matching the reference mod's feel. */
    private static final float BUTTON_GROWTH_SPEED = 5.0F;
    private static final float SEGMENT_GROWTH_SPEED = 25.0F;
    private static final float SEGMENT_DELAY = 1.0F;

    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_DETAIL = 0xFFBFBFBF;
    private static final int COLOR_EMPTY = 0xFF8A8A8A;
    private static final int COLOR_HINT = 0xFFD8A0FF;

    private int timeIn;
    private int slotHovered = -1;
    private int slotSelected = -1;
    private static boolean staysOpen;

    private ExtendedButton renameButton;
    private ExtendedButton deleteButton;
    private ExtendedButton teleportButton;

    public KamuiNavigationScreen() {
        super(Component.translatable("screen.tobimod.kamui_navigation"));
    }

    /**
     * Angle in degrees from the wheel centre to the cursor, measured clockwise
     * from the positive X axis so it lines up with slice ordering.
     */
    private static float mouseAngle(int centerX, int centerY, int mouseX, int mouseY) {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        if (dx == 0.0F && dy == 0.0F) {
            return 0.0F;
        }

        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) (Math.acos(dx / length) * (180.0D / Math.PI));
        return mouseY < centerY ? 360.0F - angle : angle;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally blank: the wheel draws over the live world.
    }

    @Override
    public void init() {
        super.init();

        // Reset interaction state in case the screen is re-initialised while
        // the mouse is already over a slice (e.g. returning from the editor).
        slotHovered = -1;
        slotSelected = -1;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Centre: navigation only. Never teleports and never starts a channel.
        addRenderableWidget(new ExtendedButton(
                centerX - 52,
                centerY - 9,
                104,
                18,
                Component.translatable("screen.tobimod.choose_coordinates"),
                button -> openManualCoordinates()
        ));

        // Side button: context-sensitive Enter Kamui / Return to Origin.
        ExtendedButton selfKamuiButton = new ExtendedButton(
                centerX - RADIUS_MAX - 110,
                centerY - 34,
                104,
                18,
                selfKamuiLabel(),
                button -> {}
        );
        selfKamuiButton.active = false;
        addRenderableWidget(selfKamuiButton);

        teleportButton = new ExtendedButton(
                centerX - RADIUS_MAX - 110,
                centerY - 10,
                104,
                18,
                Component.translatable("screen.tobimod.travel_to_waypoint"),
                button -> {}
        );
        teleportButton.active = false;
        addRenderableWidget(teleportButton);

        renameButton = new ExtendedButton(
                centerX + RADIUS_MAX + 6,
                centerY - 34,
                104,
                18,
                Component.translatable("screen.tobimod.rename_waypoint"),
                button -> openEditor(false)
        );
        addRenderableWidget(renameButton);

        deleteButton = new ExtendedButton(
                centerX + RADIUS_MAX + 6,
                centerY - 10,
                104,
                18,
                Component.translatable("screen.tobimod.delete_waypoint"),
                button -> deleteSelected()
        );
        addRenderableWidget(deleteButton);

        // Stay-open toggle, so the wheel supports both hold-to-select and
        // click-to-select without a second keybind.
        addRenderableWidget(new ExtendedButton(
                centerX + RADIUS_MAX + 6,
                centerY + 14,
                104,
                18,
                stayOpenLabel(),
                button -> {
                    staysOpen = !staysOpen;
                    button.setMessage(stayOpenLabel());
                }
        ));

        refreshButtonStates();
    }

    private Component selfKamuiLabel() {
        return isInsideKamuiVoid()
                ? Component.translatable("screen.tobimod.return_to_origin")
                : Component.translatable("screen.tobimod.enter_kamui");
    }

    private Component stayOpenLabel() {
        return Component.translatable(staysOpen
                ? "screen.tobimod.stay_open_on"
                : "screen.tobimod.stay_open_off");
    }

    private boolean isInsideKamuiVoid() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        return minecraft.player.level().dimension().location().getPath().equals("kamui_void");
    }

    private KamuiWaypoints waypoints() {
        return WaypointSyncPayload.clientWaypoints();
    }

    /** Enables management buttons only when a real, filled slot is selected. */
    private void refreshButtonStates() {
        boolean hasSelection = KamuiWaypoints.isValidSlot(slotSelected);
        boolean filled = hasSelection && !waypoints().get(slotSelected).isEmpty();

        if (renameButton != null) {
            renameButton.active = filled;
        }
        if (deleteButton != null) {
            deleteButton.active = filled;
        }
        if (teleportButton != null) {
            // Deliberately inert until the shared channel system lands.
            teleportButton.active = false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack matrices = guiGraphics.pose();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        float fract = Math.min(BUTTON_GROWTH_SPEED, this.timeIn + partialTicks) / BUTTON_GROWTH_SPEED;

        matrices.pushPose();
        matrices.translate((1 - fract) * centerX, (1 - fract) * centerY, 0);
        matrices.scale(fract, fract, fract);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        matrices.popPose();

        boolean inRange = isInRange(mouseX, mouseY);
        float angle = mouseAngle(centerX, centerY, mouseX, mouseY);
        float degPer = 360.0F / SEGMENTS;
        float totalDeg = 0.0F;

        // Recomputed every frame so the cursor leaving the ring clears it.
        this.slotHovered = -1;

        for (int seg = 0; seg < SEGMENTS; seg++) {
            KamuiWaypoint waypoint = waypoints().get(seg);
            boolean mouseInSector = inRange && angle > totalDeg && angle < totalDeg + degPer;
            if (mouseInSector) {
                this.slotHovered = seg;
            }

            float radius = Math.max(0.0F, Math.min(
                    (this.timeIn + partialTicks - seg * SEGMENT_DELAY / SEGMENTS) * SEGMENT_GROWTH_SPEED,
                    RADIUS_MAX
            ));

            float gs = seg % 2 == 0 ? 0.35F : 0.25F;
            float r = gs;
            float g = gs;
            float b = gs;
            float a = 0.4F;

            if (mouseInSector) {
                r = g = b = 1.0F;
            }
            if (seg == slotSelected) {
                r = 0.75F;
                g = 0.45F;
                b = 1.0F;
                a = 0.6F;
            }

            drawSlice(matrices, centerX, centerY, totalDeg, degPer, radius, r, g, b, a);
            totalDeg += degPer;

            drawSliceLabels(guiGraphics, matrices, centerX, centerY, totalDeg, degPer, seg, waypoint);
        }

        drawFooterHint(guiGraphics, centerX, centerY);
    }

    private void drawSlice(
            PoseStack matrices,
            int centerX,
            int centerY,
            float totalDeg,
            float degPer,
            float radius,
            float r,
            float g,
            float b,
            float a
    ) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(TobiRenderTypes.TRIANGLE_STRIP);
        Matrix4f pose = matrices.last().pose();

        for (float i = degPer; i >= 0; i--) {
            float rad = (float) ((i + totalDeg) / 180.0F * Math.PI);
            float outerX = (float) (centerX + Math.cos(rad) * radius);
            float outerY = (float) (centerY + Math.sin(rad) * radius);
            float innerX = (float) (centerX + Math.cos(rad) * radius / 2.3F);
            float innerY = (float) (centerY + Math.sin(rad) * radius / 2.3F);

            buffer.addVertex(pose, innerX, innerY, 0).setColor(r, g, b, a);
            buffer.addVertex(pose, outerX, outerY, 0).setColor(r, g, b, a);
        }

        bufferSource.endBatch(TobiRenderTypes.TRIANGLE_STRIP);
    }

    /** Draws name, dimension and coordinates rotated to follow the slice. */
    private void drawSliceLabels(
            GuiGraphics guiGraphics,
            PoseStack matrices,
            int centerX,
            int centerY,
            float totalDeg,
            float degPer,
            int seg,
            KamuiWaypoint waypoint
    ) {
        float labelAngle = (totalDeg - degPer / 2) * (float) Math.PI / 180.0F;
        float labelX = centerX + (float) (Math.cos(labelAngle) * (RADIUS_MAX / 1.4D));
        float labelY = centerY + (float) (Math.sin(labelAngle) * (RADIUS_MAX / 1.4D));
        boolean upsideDown = labelAngle > Math.PI / 2 && labelAngle < 3 * Math.PI / 2;

        String name = waypoint.isEmpty() ? "+" : waypoint.name();
        int nameColor = waypoint.isEmpty() ? COLOR_EMPTY : COLOR_NAME;

        matrices.pushPose();
        matrices.translate(labelX, labelY, 0);
        matrices.scale(0.85F, 0.85F, 0.85F);
        matrices.mulPose(Axis.ZP.rotation(upsideDown ? labelAngle + (float) Math.PI : labelAngle));
        guiGraphics.drawString(this.font, name, -this.font.width(name) / 2, -15, nameColor);
        matrices.popPose();

        if (waypoint.isEmpty()) {
            return;
        }

        String dimension = waypoint.dimensionLabel();
        String coordinates = waypoint.coordinateLabel();

        matrices.pushPose();
        matrices.translate(labelX, labelY, 0);
        matrices.scale(0.7F, 0.7F, 0.7F);
        matrices.mulPose(Axis.ZP.rotation(upsideDown ? labelAngle + (float) Math.PI : labelAngle));
        guiGraphics.drawString(this.font, dimension, -this.font.width(dimension) / 2, -5, COLOR_DETAIL);
        guiGraphics.drawString(this.font, coordinates, -this.font.width(coordinates) / 2, 10, COLOR_DETAIL);
        matrices.popPose();
    }

    private void drawFooterHint(GuiGraphics guiGraphics, int centerX, int centerY) {
        Component hint = Component.translatable("screen.tobimod.navigation_hint");
        int width = this.font.width(hint);
        guiGraphics.drawString(this.font, hint, centerX - width / 2, centerY + RADIUS_MAX + 14, COLOR_HINT);
    }

    /** True when the cursor is inside the ring, outside the centre dead zone. */
    public boolean isInRange(double mouseX, double mouseY) {
        double dist = new Vec3(this.width / 2.0D, this.height / 2.0D, 0.0D)
                .distanceTo(new Vec3(mouseX, mouseY, 0.0D));
        return dist > RADIUS_MIN && dist < RADIUS_MAX;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Grace period: ignore clicks for the first few ticks after the screen
        // opens so a held-down or quickly re-pressed mouse button from a
        // sub-screen (editor, manual coords) cannot accidentally select a slot.
        if (timeIn < 5) {
            return true;
        }

        if (isInRange(mouseX, mouseY) && slotHovered != -1) {
            selectSlot(slotHovered);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Swallow releases during the opening grace period so a held-down click
        // from a sub-screen cannot arm a widget on the freshly-opened wheel.
        if (timeIn < 5) {
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Selecting an empty slot opens the editor to save the current position.
     * Selecting a filled slot just highlights it for rename/delete.
     */
    private void selectSlot(int slot) {
        if (!KamuiWaypoints.isValidSlot(slot)) {
            return;
        }

        slotSelected = slot;
        refreshButtonStates();

        if (waypoints().get(slot).isEmpty()) {
            openEditor(true);
        }
    }

    private void openEditor(boolean creating) {
        if (!KamuiWaypoints.isValidSlot(slotSelected)) {
            return;
        }

        Minecraft.getInstance().setScreen(new WaypointEditorScreen(slotSelected, creating));
    }

    private void openManualCoordinates() {
        Minecraft.getInstance().setScreen(new ManualTeleportScreen());
    }

    private void deleteSelected() {
        if (!KamuiWaypoints.isValidSlot(slotSelected) || waypoints().get(slotSelected).isEmpty()) {
            return;
        }

        PacketDistributor.sendToServer(WaypointActionPayload.delete(slotSelected));
        refreshButtonStates();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        // Swallow the opening key while the wheel is up so it cannot re-trigger.
        if (keyCode == ModKeybindings.KAMUI_NAVIGATION.getKey().getValue()) {
            if (staysOpen) {
                onClose();
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        // Hold-to-select: releasing the key commits the hovered slot. The
        // stay-open toggle switches to plain click-to-select instead.
        if (!staysOpen && this.timeIn > 1) {
            long window = Minecraft.getInstance().getWindow().getWindow();
            if (!InputConstants.isKeyDown(window, ModKeybindings.KAMUI_NAVIGATION.getKey().getValue())) {
                if (slotHovered != -1) {
                    selectSlot(slotHovered);
                    // selectSlot may have opened the editor; only close if it did not.
                    if (Minecraft.getInstance().screen == this) {
                        onClose();
                    }
                } else {
                    onClose();
                }
                return;
            }
        }

        this.timeIn++;
        refreshButtonStates();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}