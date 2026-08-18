package com.tobi.tobimod.common.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class TobiPlayerData implements INBTSerializable<CompoundTag> {

    // Kamui Intangibility
    private boolean isIntangible = false;
    private int intangibilityTimeLeft = 0;
    private int intangibilityCooldown = 0;

    // Kamui Warp
    private double savedX = 0;
    private double savedY = 0;
    private double savedZ = 0;
    private String savedDimension = "";
    private boolean hasSavedLocation = false;

    // Waypoints (10 locations)
    private WaypointData[] waypoints = new WaypointData[10];

    // Genjutsu
    private int genjutsuCooldown = 0;
    private int advancedGenjutsuCooldown = 0;

    // Black Receivers
    private boolean hasBlackReceiver = false;
    private int drainCooldown = 0;

    // Getters and setters for all fields...

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("isIntangible", isIntangible);
        tag.putInt("intangibilityTimeLeft", intangibilityTimeLeft);
        tag.putInt("intangibilityCooldown", intangibilityCooldown);

        tag.putDouble("savedX", savedX);
        tag.putDouble("savedY", savedY);
        tag.putDouble("savedZ", savedZ);
        tag.putString("savedDimension", savedDimension);
        tag.putBoolean("hasSavedLocation", hasSavedLocation);

        // Save waypoints...
        // Save other data...

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        isIntangible = tag.getBoolean("isIntangible");
        intangibilityTimeLeft = tag.getInt("intangibilityTimeLeft");
        intangibilityCooldown = tag.getInt("intangibilityCooldown");

        savedX = tag.getDouble("savedX");
        savedY = tag.getDouble("savedY");
        savedZ = tag.getDouble("savedZ");
        savedDimension = tag.getString("savedDimension");
        hasSavedLocation = tag.getBoolean("hasSavedLocation");

        // Load waypoints...
        // Load other data...
    }

    public static class WaypointData {
        public String name;
        public double x, y, z;
        public String dimension;
        // Constructor, getters, setters...
    }
}