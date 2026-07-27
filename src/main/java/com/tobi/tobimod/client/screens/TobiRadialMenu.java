/**
 * Radial Menu for Tobi Mod Abilities
 * Adapted from AdvPortalRadialMenu by Vazkii (Psi mod)
 */
package com.tobi.tobimod.client.screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;

public class TobiRadialMenu extends Screen {
    // 12 total segments: 8 abilities + 4 empty for future expansion
    private static final int SEGMENTS = 12;
    
    // Ability names for the 8 active abilities
    private static final String[] ABILITY_NAMES = {
        "Kamui Intangibility",
        "Kamui Travel",
        "Kamui Attack",
        "Basic Genjutsu",
        "Advanced Genjutsu",
        "Kamui Warp",
        "Location Marker",
        "Black Receivers",
        "Empty", // Future ability slot
        "Empty", // Future ability slot
        "Empty", // Future ability slot
        "Empty"  // Future ability slot
    };
    
    // Ability descriptions
    private static final String[] ABILITY_DESCRIPTIONS = {
        "Phase through attacks",
        "Teleport to coordinates",
        "Kidnap enemies",
        "Freeze enemy gaze",
        "Mind control",
        "Dimensional teleport",
        "Set waypoints",
        "Drain life force",
        "Coming Soon",
        "Coming Soon",
        "Coming Soon",
        "Coming Soon"
    };

    private int timeIn = 0;
    private int slotHovered = -1;
    private int slotSelected = -1;
    private final static int radiusMin = 26;
    private final static int radiusMax = 120;
    private boolean staysOpen = false; // Hold mode (false) or Press mode (true)

    // Toggle button for hold/press mode
    private ToggleModeButton toggleModeButton;

    public TobiRadialMenu() {
        super(Component.literal("Tobi Abilities"));
        // Load saved preference for hold/press mode
        this.staysOpen = loadStayOpenPreference();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No background rendering (transparent)
    }

    @Override
    public void init() {
        // Add the hold/press mode toggle button in the top right
        int buttonSize = 20;
        toggleModeButton = new ToggleModeButton(
            width / 2 + 140, 
            height / 2 - 80, 
            buttonSize, 
            buttonSize, 
            staysOpen,
            (button) -> {
                staysOpen = !staysOpen;
                saveStayOpenPreference(staysOpen);
                ((ToggleModeButton) button).setActive(staysOpen);
            }
        );
        addRenderableWidget(toggleModeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mx, int my, float partialTicks) {
        PoseStack matrices = guiGraphics.pose();
        float speedOfButtonGrowth = 5f;
        float fract = Math.min(speedOfButtonGrowth, this.timeIn + partialTicks) / speedOfButtonGrowth;
        int x = this.width / 2;
        int y = this.height / 2;

        boolean inRange = isInRange(mx, my);

        // Animate side buttons
        matrices.pushPose();
        matrices.translate((1 - fract) * x, (1 - fract) * y, 0);
        matrices.scale(fract, fract, fract);
        super.render(guiGraphics, mx, my, partialTicks);
        matrices.popPose();

        float angle = mouseAngle(x, y, mx, my);
        float totalDeg = 0;
        float degPer = 360F / SEGMENTS;

        slotHovered = -1; // Reset hover state

        for (int seg = 0; seg < SEGMENTS; seg++) {
            String abilityName = ABILITY_NAMES[seg];
            String abilityDesc = ABILITY_DESCRIPTIONS[seg];
            
            boolean mouseInSector = this.isCursorInSlice(angle, totalDeg, degPer, inRange);
            float delayBetweenSegments = 1f;
            float speedOfSegmentGrowth = 25f;
            float radius = Math.max(0F, Math.min((this.timeIn + partialTicks - seg * delayBetweenSegments / SEGMENTS) * speedOfSegmentGrowth, radiusMax));
            
            // Color calculations
            float gs = 0.25F;
            if (seg % 2 == 0) {
                gs += 0.1F;
            }

            float r = gs;
            float g = gs;
            float b = gs;
            float a = 0.4F;
            
            // Empty slots are darker
            if (seg >= 8) {
                r = g = b = 0.15F;
                a = 0.3F;
            }
            
            if (mouseInSector && seg < 8) { // Only highlight active abilities
                this.slotHovered = seg;
                r = g = b = 1F;
            }

            // Draw the pie slice
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.gui());

            for (float i = degPer; i >= 0; i--) {
                float rad = (float) ((i + totalDeg) / 180F * Math.PI);
                float xp = (float) (x + Math.cos(rad) * radius);
                float yp = (float) (y + Math.sin(rad) * radius);

                Matrix4f pose = matrices.last().pose();
                buffer.addVertex(pose, (float) (x + Math.cos(rad) * radius / 2.3F), (float) (y + Math.sin(rad) * radius / 2.3F), 0).setColor(r, g, b, a);
                buffer.addVertex(pose, xp, yp, 0).setColor(r, g, b, a);
            }

            bufferSource.endBatch();
            totalDeg += degPer;

            // Draw ability name in each slice
            float nameAngle = (totalDeg - degPer / 2) * (float) Math.PI / 180F;
            float nameX = x + (float) (Math.cos(nameAngle) * (radiusMax / 1.5));
            float nameY = y + (float) (Math.sin(nameAngle) * (radiusMax / 1.5));
            int textWidth = this.font.width(abilityName);
            int descWidth = this.font.width(abilityDesc);

            matrices.pushPose();
            matrices.translate(nameX, nameY, 0);
            matrices.scale(0.75f, 0.75f, 0.75f);
            
            // Rotate text to be readable
            if (nameAngle > Math.PI / 2 && nameAngle < 3 * Math.PI / 2) {
                matrices.mulPose(Axis.ZP.rotation(nameAngle + (float) Math.PI));
            } else {
                matrices.mulPose(Axis.ZP.rotation(nameAngle));
            }
            
            // Draw ability name
            int nameColor = seg >= 8 ? Color.DARK_GRAY.getRGB() : Color.WHITE.getRGB();
            guiGraphics.drawString(this.font, abilityName, -textWidth / 2, -10, nameColor);
            
            // Draw description
            matrices.scale(0.8f, 0.8f, 0.8f);
            int descColor = seg >= 8 ? Color.DARK_GRAY.getRGB() : Color.LIGHT_GRAY.getRGB();
            guiGraphics.drawString(this.font, abilityDesc, (int)(-descWidth / 2 * 0.8f), 5, descColor);
            
            matrices.popPose();
        }

        // Draw instruction text at bottom
        String instruction = staysOpen ? "Click ability or press [Z] to close" : "Release [Z] to select";
        int instructionWidth = this.font.width(instruction);
        guiGraphics.drawString(this.font, instruction, x - instructionWidth / 2, y + radiusMax + 15, Color.YELLOW.getRGB());
    }

    private static float mouseAngle(int x, int y, int mx, int my) {
        Vector2f baseVec = new Vector2f(1F, 0F);
        Vector2f mouseVec = new Vector2f(mx - x, my - y);

        float ang = (float) (Math.acos(baseVec.dot(mouseVec) / (baseVec.length() * mouseVec.length())) * (180F / Math.PI));
        return my < y ? 360F - ang : ang;
    }

    private boolean isCursorInSlice(float angle, float totalDeg, float degPer, boolean inRange) {
        return inRange && angle > totalDeg && angle < totalDeg + degPer;
    }

    public boolean isInRange(double mouseX, double mouseY) {
        int x = this.width / 2;
        int y = this.height / 2;

        double dist = new Vec3(x, y, 0).distanceTo(new Vec3(mouseX, mouseY, 0));
        return dist > radiusMin && dist < radiusMax;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isInRange(mouseX, mouseY) && slotHovered >= 0 && slotHovered < 8) {
            activateAbility(slotHovered);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC or keybind to close (only in press mode)
        if (staysOpen && (keyCode == 256 || keyCode == ModKeybindings.LOCATION_MARKER.getKey().getValue())) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        // In hold mode, close when key is released
        if (!staysOpen && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), ModKeybindings.LOCATION_MARKER.getKey().getValue())) {
            // Key released, activate hovered ability
            if (slotHovered >= 0 && slotHovered < 8) {
                activateAbility(slotHovered);
            }
            onClose();
        }

        this.timeIn++;
    }

    private void activateAbility(int slot) {
        switch (slot) {
            case 0: // Kamui Intangibility
                TobiMod.LOGGER.info("Activated: Kamui Intangibility");
                // TODO: Implement ability logic
                break;
            case 1: // Kamui Travel - Open coordinate GUI
                Minecraft.getInstance().setScreen(new KamuiTravelMenu());
                return; // Don't close menu yet
            case 2: // Kamui Attack
                TobiMod.LOGGER.info("Activated: Kamui Attack");
                // TODO: Implement ability logic
                break;
            case 3: // Basic Genjutsu
                TobiMod.LOGGER.info("Activated: Basic Genjutsu");
                // TODO: Implement ability logic
                break;
            case 4: // Advanced Genjutsu
                TobiMod.LOGGER.info("Activated: Advanced Genjutsu");
                // TODO: Implement ability logic
                break;
            case 5: // Kamui Warp
                TobiMod.LOGGER.info("Activated: Kamui Warp");
                // TODO: Implement ability logic
                break;
            case 6: // Location Marker
                TobiMod.LOGGER.info("Activated: Location Marker");
                // TODO: Implement ability logic
                break;
            case 7: // Black Receivers
                TobiMod.LOGGER.info("Activated: Black Receivers");
                // TODO: Implement ability logic
                break;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Simple preference storage (you can expand this to save to file later)
    private boolean loadStayOpenPreference() {
        // For now, default to hold mode (false)
        // TODO: Load from config file
        return false;
    }

    private void saveStayOpenPreference(boolean staysOpen) {
        // TODO: Save to config file
        TobiMod.LOGGER.info("Mode changed to: " + (staysOpen ? "Press" : "Hold"));
    }

    // Simple vector class for calculations
    private static class Vector2f {
        public float x;
        public float y;

        public Vector2f(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public final float dot(Vector2f v1) {
            return (this.x * v1.x + this.y * v1.y);
        }

        public final float length() {
            return (float) Math.sqrt(this.x * this.x + this.y * this.y);
        }
    }

    // Simple toggle button for hold/press mode
    private static class ToggleModeButton extends net.minecraft.client.gui.components.Button {
        private boolean active;

        public ToggleModeButton(int x, int y, int width, int height, boolean initialState, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.active = initialState;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Draw simple toggle indicator
            int color = active ? Color.GREEN.getRGB() : Color.RED.getRGB();
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            
            // Draw border
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, Color.WHITE.getRGB());
            guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, Color.WHITE.getRGB());
            guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, Color.WHITE.getRGB());
            guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, Color.WHITE.getRGB());
            
            // Draw text
            String text = active ? "P" : "H";
            int textWidth = Minecraft.getInstance().font.width(text);
            guiGraphics.drawString(Minecraft.getInstance().font, text, 
                getX() + (width - textWidth) / 2, 
                getY() + (height - 8) / 2, 
                Color.WHITE.getRGB());
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int padding, int color) {
            // Override to show tooltip
        }
    }
}
