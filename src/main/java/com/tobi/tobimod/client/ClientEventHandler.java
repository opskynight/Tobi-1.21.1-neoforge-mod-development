package com.tobi.tobimod.client;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.screens.KamuiNavigationScreen;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import com.tobi.tobimod.network.payload.KamuiJumpPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side Kamui enforcement, jump detection, and floor prediction.
 *
 * <p>Jump behavior:
 * <ul>
 *   <li><b>Phase Ascend</b>: Look up 45°+ + inside wall + tap jump →
 *       instant teleport to surface.</li>
 *   <li><b>Step-Up</b>: Hold jump → +1 every 4 ticks with solid-below check.
 *       Uses GLFW direct key state to bypass Minecraft's KeyMapping resets.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("KamuiDebug");
    private static final double FLOOR_EPSILON = 0.05D;
    private static final int STEP_UP_TICKS = 4;
    private static final float LOOK_UP_PITCH = -45.0F;
    private static final int CLIENT_SCAN_DEPTH = 128;

    private static boolean jumpHeld = false;
    private static int jumpHoldCounter = 0;
    private static boolean phaseAscendFired = false;
    private static boolean suppressVanillaJump = false;

    private ClientEventHandler() {}

    public static void beginEnterChannel() {}

    public static void resetJumpTracking() {
        jumpHeld = false;
        jumpHoldCounter = 0;
        phaseAscendFired = false;
        suppressVanillaJump = false;
    }

    /**
     * Checks if the jump key is physically held down using GLFW directly.
     * This bypasses Minecraft's {@code KeyMapping.isDown()} which can get
     * reset by the game's input processing after we manipulate player state.
     */
    private static boolean isJumpKeyPhysicallyHeld(Minecraft minecraft) {
        InputConstants.Key key = minecraft.options.keyJump.getKey();
        long window = minecraft.getWindow().getWindow();

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_TRUE;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_TRUE;
        }

        // Fallback to Minecraft's key binding
        return minecraft.options.keyJump.isDown();
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        KamuiIntangibilityStatePayload.tickClientTimer();

        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            Player player = minecraft.player;
            double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
            double yVel = player.getDeltaMovement().y;
            boolean onFloor = Math.abs(player.getY() - floorY) < 0.1;

            // Client-side enforcement: noPhysics + jitter prevention
            player.noPhysics = true;
            player.resetFallDistance();

            if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
                player.setPos(player.getX(), floorY, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
                player.setNoGravity(true);
            }

            // ══════════════════════════════════════════
            //  Jump Detection — GLFW direct key state
            // ══════════════════════════════════════════
            boolean isJumpKeyDown = isJumpKeyPhysicallyHeld(minecraft);
            boolean isLookingUp = player.getXRot() < LOOK_UP_PITCH;

            if (isJumpKeyDown) {
                if (!jumpHeld) {
                    jumpHeld = true;
                    jumpHoldCounter = 0;
                    phaseAscendFired = false;
                }

                jumpHoldCounter++;

                // ── Phase Ascend: look up + inside wall + first tick + on floor ──
                if (jumpHoldCounter == 1 && onFloor && isLookingUp && isFeetInsideSolidClient(player)) {
                    double currentFloor = KamuiIntangibilityStatePayload.getClientFloorY();
                    double surfaceY = findSurfaceAboveClient(player, currentFloor);
                    if (surfaceY > currentFloor) {
                        LOGGER.info("[KAMUI] Phase Ascend! floor {} -> surface {}", currentFloor, surfaceY);
                        PacketDistributor.sendToServer(new KamuiJumpPayload(false));
                        KamuiIntangibilityStatePayload.predictFloorSet(surfaceY);
                        snapToFloor(player);
                        phaseAscendFired = true;
                        suppressVanillaJump = true;
                    }
                }

                // ── Step-Up: +1 raise every STEP_UP_TICKS ──
                if (!phaseAscendFired) {
                    if (jumpHoldCounter == 1 || (jumpHoldCounter > 1 && (jumpHoldCounter - 1) % STEP_UP_TICKS == 0)) {
                        double currentFloor = KamuiIntangibilityStatePayload.getClientFloorY();
                        double newFloorY = currentFloor + 1.0D;
                        boolean solidBelow = isSolidBelowClient(player, newFloorY);

                        LOGGER.info("[KAMUI] Step-Up | tick={} floor={} newFloor={} solidBelow={}",
                                jumpHoldCounter, String.format("%.1f", currentFloor),
                                String.format("%.1f", newFloorY), solidBelow);

                        if (solidBelow) {
                            PacketDistributor.sendToServer(new KamuiJumpPayload(true));
                            KamuiIntangibilityStatePayload.predictFloorRaise();
                            snapToFloor(player);
                            suppressVanillaJump = true;
                            LOGGER.info("[KAMUI] Step-Up RAISED to {}", KamuiIntangibilityStatePayload.getClientFloorY());
                        }
                    }
                }
            } else {
                if (jumpHeld) {
                    LOGGER.info("[KAMUI] Jump released | held={} ticks", jumpHoldCounter);
                }
                jumpHeld = false;
                jumpHoldCounter = 0;
                phaseAscendFired = false;
            }

            // Suppress vanilla jump after floor-raise
            if (suppressVanillaJump) {
                player.setOnGround(false);
                suppressVanillaJump = false;
            }
        } else {
            jumpHeld = false;
            jumpHoldCounter = 0;
            phaseAscendFired = false;
            suppressVanillaJump = false;
        }

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

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            minecraft.player.setNoGravity(false);

            double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
            if (minecraft.player.getY() < floorY) {
                minecraft.player.setPos(minecraft.player.getX(), floorY, minecraft.player.getZ());
                minecraft.player.setDeltaMovement(
                        minecraft.player.getDeltaMovement().x,
                        0.0D,
                        minecraft.player.getDeltaMovement().z
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
        BlockState state = player.level().getBlockState(feetPos);
        return !state.getCollisionShape(player.level(), feetPos).isEmpty();
    }

    private static boolean isSolidBelowClient(Player player, double newFloorY) {
        BlockPos belowNewFloor = BlockPos.containing(player.getX(), newFloorY - 1.0D, player.getZ());
        BlockState state = player.level().getBlockState(belowNewFloor);
        return !state.getCollisionShape(player.level(), belowNewFloor).isEmpty();
    }

    private static double findSurfaceAboveClient(Player player, double fromY) {
        int blockX = Mth.floor(player.getX());
        int blockZ = Mth.floor(player.getZ());
        int startY = Mth.floor(fromY) + 1;

        for (int y = startY; y < startY + CLIENT_SCAN_DEPTH; y++) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            BlockState blockState = player.level().getBlockState(pos);

            if (blockState.getCollisionShape(player.level(), pos).isEmpty()) {
                BlockPos belowPos = pos.below();
                BlockState belowState = player.level().getBlockState(belowPos);
                if (!belowState.getCollisionShape(player.level(), belowPos).isEmpty()) {
                    return y - 1.0D;
                }
            }
        }

        return fromY;
    }
}