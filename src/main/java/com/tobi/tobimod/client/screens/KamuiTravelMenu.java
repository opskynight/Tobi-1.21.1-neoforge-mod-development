package com.tobi.tobimod.client.screens;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;

import java.util.function.Predicate;

public class KamuiTravelMenu extends Screen {
    private int ticksOpened = 0;
    private EditBox nameField;
    private EditBox xPos;
    private EditBox yPos;
    private EditBox zPos;
    
    // Validator for coordinate input
    private final Predicate<String> coordinateValidator = this::isValidCoordinate;

    public KamuiTravelMenu() {
        super(Component.literal("Kamui Travel"));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent dark background
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void init() {
        super.init();
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Location name field
        this.nameField = new EditBox(
            this.font, 
            centerX - 100, 
            centerY - 50, 
            200, 
            20, 
            Component.literal("Type Location")
        );
        this.nameField.setMaxLength(20);
        this.nameField.setValue("Type Location");
        this.nameField.setVisible(true);
        addRenderableWidget(nameField);
        
        // X coordinate field
        this.xPos = new EditBox(
            this.font, 
            centerX - 100, 
            centerY - 20, 
            60, 
            20, 
            Component.literal("X")
        );
        this.xPos.setValue("0");
        this.xPos.setFilter(coordinateValidator);
        this.xPos.setVisible(true);
        addRenderableWidget(xPos);
        
        // Y coordinate field
        this.yPos = new EditBox(
            this.font, 
            centerX - 30, 
            centerY - 20, 
            60, 
            20, 
            Component.literal("Y")
        );
        this.yPos.setValue("64");
        this.yPos.setFilter(coordinateValidator);
        this.yPos.setVisible(true);
        addRenderableWidget(yPos);
        
        // Z coordinate field
        this.zPos = new EditBox(
            this.font, 
            centerX + 40, 
            centerY - 20, 
            60, 
            20, 
            Component.literal("Z")
        );
        this.zPos.setValue("0");
        this.zPos.setFilter(coordinateValidator);
        this.zPos.setVisible(true);
        addRenderableWidget(zPos);
        
        // Warp to Location button
        ExtendedButton warpButton = new ExtendedButton(
            centerX - 100, 
            centerY + 20, 
            130, 
            20, 
            Component.literal("Warp to Location"), 
            (button) -> {
                warpToLocation();
            }
        );
        addRenderableWidget(warpButton);
        
        // Cancel button
        ExtendedButton cancelButton = new ExtendedButton(
            centerX + 40, 
            centerY + 20, 
            60, 
            20, 
            Component.literal("Cancel"), 
            (button) -> {
                onClose();
            }
        );
        addRenderableWidget(cancelButton);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.nameField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render background first
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render title
        int centerX = width / 2;
        int centerY = height / 2;
        
        String title = "Kamui Travel - Enter Coordinates";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, centerX - titleWidth / 2, centerY - 70, 0xFFFFFF);
        
        // Render field labels
        guiGraphics.drawString(this.font, "Location Name:", centerX - 100, centerY - 62, 0xAAAAAA);
        guiGraphics.drawString(this.font, "X:", centerX - 115, centerY - 15, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Y:", centerX - 45, centerY - 15, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Z:", centerX + 25, centerY - 15, 0xAAAAAA);
        
        // Render all widgets
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Prevent closing with keybind for first few ticks
        if (ticksOpened < 20 && keyCode == ModKeybindings.LOCATION_MARKER.getKey().getValue()) {
            return true;
        }
        
        // ESC to close
        if (keyCode == 256) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        ticksOpened++;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Prevent keybind character from being typed in first few ticks
        String toggleToolKeybind = ModKeybindings.LOCATION_MARKER.getKey().getName();
        if (ticksOpened < 20 && toggleToolKeybind.length() > 0 && 
            toggleToolKeybind.charAt(toggleToolKeybind.length() - 1) == codePoint) {
            return false;
        }
        return this.getFocused() != null && this.getFocused().charTyped(codePoint, modifiers);
    }

    private void warpToLocation() {
        try {
            // Parse coordinates
            double x = parseCoordinate(xPos.getValue());
            double y = parseCoordinate(yPos.getValue());
            double z = parseCoordinate(zPos.getValue());
            String locationName = nameField.getValue();
            
            // Validate coordinates
            if (!isValidYCoordinate(y)) {
                TobiMod.LOGGER.warn("Invalid Y coordinate: " + y);
                return;
            }
            
            TobiMod.LOGGER.info("Warping to: " + locationName + " at (" + x + ", " + y + ", " + z + ")");
            
            // TODO: Send packet to server to teleport player
            // PacketDistributor.sendToServer(new KamuiTravelPayload(x, y, z, locationName));
            
            // Close the GUI
            this.onClose();
            
        } catch (NumberFormatException e) {
            TobiMod.LOGGER.error("Invalid coordinate format");
        }
    }

    private double parseCoordinate(String value) throws NumberFormatException {
        if (value.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(value);
    }

    private boolean isValidCoordinate(String input) {
        if (input.isEmpty()) return true;
        
        // Allow negative sign at start
        if (input.equals("-")) return true;
        
        // Allow decimal point
        if (input.endsWith(".")) return true;
        
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidYCoordinate(double y) {
        // Minecraft Y coordinate limits (adjust based on version)
        return y >= -64 && y <= 320;
    }
}
