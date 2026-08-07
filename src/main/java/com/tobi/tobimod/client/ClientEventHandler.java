package com.tobi.tobimod.client;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.screens.KamuiNavigationScreen;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
import com.tobi.tobimod.network.payload.KamuiVerticalMovePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side Kamui enforcement with dual-mode support.
 *
 * <h3>Underground mode</h3>
 * <ul>
 *   <li>Hold jump → smooth +0.15/tick upward (direct position)</li>
 *   <li>Hold shift → smooth -0.15/tick downward (direct position)</li>
 *   <li>Release jump → round Y up to integer</li>
 *   <li>Release shift → round Y down to integer</li>
 *   <li>Look up + tap jump → Phase Ascend packet</li>
 * </ul>
 *
 * <h3>Surface mode</h3>
 * <ul>
 *   <li>Hold jump → Step-Up (+1 every 4 ticks, solid check)</li>
 *   <li>Look up + tap jump inside wall → Phase Ascend</li>
 * </ul>
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("KamuiDebug");
    private static final double FLOOR_EPSILON = 0.05D;
    private static final int STEP_UP_TICKS = 4;
    private static final float LOOK_UP_PITCH = -45.0F;
    private static final double SMOOTH_SPEED = 0.15D;
    private static final int CLIENT_SCAN_DEPTH = 128;

    // ── Underground state ──
    private static boolean undergroundJumpHeld = false;
    private static boolean lastSentJumpHeld = false;

    // ── Surface state ──
    private static boolean jumpHeld = false;
    private static int jumpHoldCounter = 0;
    private static boolean phaseAscendFired = false;
    private static boolean suppressVanillaJump = false;

    private ClientEventHandler() {}

    public static void beginEnterChannel() {}

    public static void resetJumpTracking() {
        undergroundJumpHeld = false;
        lastSentJumpHeld = false;
        jumpHeld = false;
        jumpHoldCounter = 0;
        phaseAscendFired = false;
        suppressVanillaJump = false;
    }

    /** Check physical key state via GLFW (bypasses Minecraft KeyMapping resets). */
    private static boolean isJumpKeyPhysicallyHeld(Minecraft minecraft) {
        InputConstants.Key key = minecraft.options.keyJump.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_TRUE;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_TRUE;
        }
        return minecraft.options.keyJump.isDown();
    }

    /** Check physical shift key via GLFW. */
    private static boolean isShiftKeyPhysicallyHeld(Minecraft minecraft) {
        InputConstants.Key key = minecraft.options.keyShift.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_TRUE;
        }
        return minecraft.options.keyShift.isDown();
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        KamuiIntangibilityStatePayload.tickClientTimer();

        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            Player player = minecraft.player;
            player.noPhysics = true;
            player.resetFallDistance();

            boolean underground = KamuiIntangibilityStatePayload.isClientUnderground();
            boolean isJumpKeyDown = isJumpKeyPhysicallyHeld(minecraft);
            boolean isShiftKeyDown = isShiftKeyPhysicallyHeld(minecraft);
            boolean isLookingUp = player.getXRot() < LOOK_UP_PITCH;

            if (underground) {
                handleUndergroundTick(player, minecraft, isJumpKeyDown, isShiftKeyDown, isLookingUp);
            } else {
                handleSurfaceTick(player, minecraft, isJumpKeyDown, isLookingUp);
            }
        } else {
            undergroundJumpHeld = false;
            lastSentJumpHeld = false;
            jumpHeld = false;
            jumpHoldCounter = 0;
            phaseAscendFired = false;
            suppressVanillaJump = false;
        }

        // ── Keybinds ──
        while (ModKeybindings.KAMUI_INTANGIBILITY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new KamuiIntangibilityTogglePayload());
            }
        }
        while (ModKeybindings.KAMUI_NAVIGATION.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new KamuiNavigationScreen());
            }
        }
    }

    // ════════════════════════════════════════════════
    //  Underground Tick
    // ════════════════════════════════════════════════

    private static void handleUndergroundTick(Player player, Minecraft minecraft,
                                              boolean isJumpKeyDown, boolean isShiftKeyDown, boolean isLookingUp) {
        // ── Phase Ascend: look up + tap jump inside wall ──
        if (isJumpKeyDown && !undergroundJumpHeld && isLookingUp && isFeetInsideSolidClient(player)) {
            double currentFloor = KamuiIntangibilityStatePayload.getClientFloorY();
            double surfaceY = findSurfaceAboveClient(player, currentFloor);
            if (surfaceY > currentFloor) {
                PacketDistributor.sendToServer(new KamuiJumpPayload(false));
                KamuiIntangibilityStatePayload.predictFloorSet(surfaceY);
                snapToFloor(player);
                undergroundJumpHeld = true;
                return;
            }
        }

        undergroundJumpHeld = isJumpKeyDown;

        // ── Send jump state to server if changed ──
        if (undergroundJumpHeld != lastSentJumpHeld) {
            PacketDistributor.sendToServer(new KamuiVerticalMovePayload(undergroundJumpHeld));
            lastSentJumpHeld = undergroundJumpHeld;
        }

        // ── Client-side smooth movement prediction ──
        if (undergroundJumpHeld) {
            // Move up
            double newY = player.getY() + SMOOTH_SPEED;
            player.setPos(player.getX(), newY, player.getZ());
            KamuiIntangibilityStatePayload.predictFloorSet(newY);
        } else if (isShiftKeyDown) {
            // Move down
            double newY = player.getY() - SMOOTH_SPEED;
            double supportedY = findSupportBelowClient(player, newY);
            newY = Math.max(newY, supportedY);
            player.setPos(player.getX(), newY, player.getZ());
            KamuiIntangibilityStatePayload.predictFloorSet(newY);
        }

        // ── Rounding on release ──
        // Jump release rounding is handled by the server via KamuiVerticalMovePayload
        // Shift release: we track it locally
        if (!isShiftKeyDown && wasShiftHeldLastTick) {
            double roundedY = Math.floor(player.getY());
            player.setPos(player.getX(), roundedY, player.getZ());
            KamuiIntangibilityStatePayload.predictFloorSet(roundedY);
            // Also send to server so it rounds too
            PacketDistributor.sendToServer(new KamuiVerticalMovePayload(false));
        }
        wasShiftHeldLastTick = isShiftKeyDown;

        // Suppress vanilla jump
        player.setOnGround(false);
    }

    private static boolean wasShiftHeldLastTick = false;

    // ════════════════════════════════════════════════
    //  Surface Tick
    // ════════════════════════════════════════════════

    private static void handleSurfaceTick(Player player, Minecraft minecraft,
                                          boolean isJumpKeyDown, boolean isLookingUp) {
        double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
        double yVel = player.getDeltaMovement().y;
        boolean onFloor = Math.abs(player.getY() - floorY) < 0.1;

        // Jitter prevention
        if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
            player.setPos(player.getX(), floorY, player.getZ());
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            player.setOnGround(true);
            player.setNoGravity(true);
        }

        // ── Jump Detection ──
        if (isJumpKeyDown) {
            if (!jumpHeld) {
                jumpHeld = true;
                jumpHoldCounter = 0;
                phaseAscendFired = false;
            }
            jumpHoldCounter++;

            // Phase Ascend: look up + inside wall + first tick + on floor
            if (jumpHoldCounter == 1 && onFloor && isLookingUp && isFeetInsideSolidClient(player)) {
                double currentFloor = KamuiIntangibilityStatePayload.getClientFloorY();
                double surfaceY = findSurfaceAboveClient(player, currentFloor);
                if (surfaceY > currentFloor) {
                    PacketDistributor.sendToServer(new KamuiJumpPayload(false));
                    KamuiIntangibilityStatePayload.predictFloorSet(surfaceY);
                    snapToFloor(player);
                    phaseAscendFired = true;
                    suppressVanillaJump = true;
                }
            }

            // Step-Up: +1 every STEP_UP_TICKS
            if (!phaseAscendFired) {
                if (jumpHoldCounter == 1 || (jumpHoldCounter > 1 && (jumpHoldCounter - 1) % STEP_UP_TICKS == 0)) {
                    double currentFloor = KamuiIntangibilityStatePayload.getClientFloorY();
                    double newFloorY = currentFloor + 1.0D;
                    boolean solidBelow = isSolidBelowClient(player, newFloorY);

                    if (solidBelow) {
                        PacketDistributor.sendToServer(new KamuiJumpPayload(true));
                        KamuiIntangibilityStatePayload.predictFloorRaise();
                        snapToFloor(player);
                        suppressVanillaJump = true;
                    }
                }
            }
        } else {
            jumpHeld = false;
            jumpHoldCounter = 0;
            phaseAscendFired = false;
        }

        // Suppress vanilla jump after floor-raise
        if (suppressVanillaJump) {
            player.setOnGround(false);
            suppressVanillaJump = false;
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            boolean underground = KamuiIntangibilityStatePayload.isClientUnderground();

            if (!underground) {
                minecraft.player.setNoGravity(false);
            }

            // Safety-net floor clamp
            double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
            if (minecraft.player.getY() < floorY) {
                minecraft.player.setPos(minecraft.player.getX(), floorY, minecraft.player.getZ());
                minecraft.player.setDeltaMovement(
                        minecraft.player.getDeltaMovement().x, 0.0D, minecraft.player.getDeltaMovement().z
                );
                minecraft.player.setOnGround(true);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  Client-side helpers
    // ════════════════════════════════════════════════

    private static void snapToFloor(Player player) {
        double predictedFloor = KamuiIntangibilityStatePayload.getClientFloorY();
        player.setPos(player.getX(), predictedFloor, player.getZ());
        player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        player.setOnGround(true);
    }

    private static boolean isFeetInsideSolidClient(Player player) {
        BlockPos feetPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        return hasSolidOrWaterCollisionClient(player, feetPos);
    }

    private static boolean isSolidBelowClient(Player player, double newFloorY) {
        BlockPos belowNewFloor = BlockPos.containing(player.getX(), newFloorY - 1.0D, player.getZ());
        return hasSolidOrWaterCollisionClient(player, belowNewFloor);
    }

    /** Check if block has solid collision OR is a water source (for Kamui water-as-solid). */
    private static boolean hasSolidOrWaterCollisionClient(Player player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (!state.getCollisionShape(player.level(), pos).isEmpty()) return true;
        FluidState fluidState = player.level().getFluidState(pos);
        return fluidState.is(FluidTags.WATER) && fluidState.isSource();
    }

    private static double findSurfaceAboveClient(Player player, double fromY) {
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) + 1;
        for (int y = startY; y < startY + CLIENT_SCAN_DEPTH; y++) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (!hasSolidOrWaterCollisionClient(player, pos)) {
                BlockPos belowPos = pos.below();
                if (hasSolidOrWaterCollisionClient(player, belowPos)) {
                    return y - 1.0D;
                }
            }
        }
        return fromY;
    }

    private static double findSupportBelowClient(Player player, double fromY) {
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) - 1;
        for (int y = startY; y >= startY - CLIENT_SCAN_DEPTH; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (hasSolidOrWaterCollisionClient(player, pos)) {
                return y + 1.0D;
            }
        }
        return fromY;
    }
}