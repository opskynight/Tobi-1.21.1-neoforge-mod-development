package com.tobi.tobimod.client;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.screens.KamuiNavigationScreen;
import com.tobi.tobimod.client.sound.KamuiSoundManager;
import com.tobi.tobimod.network.payload.KamuiChannelCancelPayload;
import com.tobi.tobimod.network.payload.KamuiChannelSyncPayload;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side Kamui enforcement with dual-mode support + channel handling.
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
 *
 * <h3>Channel mode (travel)</h3>
 * <ul>
 *   <li>3-second channel (60 ticks) for ALL travel: enter/leave/waypoint/manual</li>
 *   <li>Plays kamui_channel.ogg (3s, non-looping, relative, no attenuation)</li>
 *   <li>If interrupted (damage, movement, R) → sound stops + channel cancels</li>
 *   <li>While channeling: attack/block/item use blocked; any movement cancels</li>
 *   <li>R while channeling → cancel channel + force-activate intangibility (bypasses cooldown)</li>
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

    // ── Channel state (client) ──
    private static boolean channelActive = false;
    private static int channelTicksRemaining = 0;
    private static double channelStartX, channelStartY, channelStartZ;
    private static final double CHANNEL_MOVEMENT_CANCEL_SQR = 0.09; // 0.3 blocks

    // ── Underground state ──
    private static boolean undergroundJumpHeld = false;
    private static boolean lastSentJumpHeld = false;

    // ── Surface state ──
    private static boolean jumpHeld = false;
    private static int jumpHoldCounter = 0;
    private static boolean phaseAscendFired = false;
    private static boolean suppressVanillaJump = false;

    private ClientEventHandler() {}

    // ──────────────────────────────────────────────
    //  Channel public API
    // ──────────────────────────────────────────────

    /** Starts the 3s channel locally (plays sound, sets freeze state). Idempotent. */
    public static void beginChannel() {
        if (channelActive) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        channelActive = true;
        channelTicksRemaining = 60;
        channelStartX = mc.player.getX();
        channelStartY = mc.player.getY();
        channelStartZ = mc.player.getZ();
        KamuiSoundManager.startChannel();
    }

    /** Legacy alias — start enter channel (called from older screen code). */
    public static void beginEnterChannel() {
        beginChannel();
    }

    /** Cancels channel locally (stops sound). Idempotent. */
    public static void cancelChannel() {
        if (!channelActive) return;
        channelActive = false;
        channelTicksRemaining = 0;
        KamuiSoundManager.stop();
    }

    /** Server -> client sync handler */
    public static void handleChannelSync(KamuiChannelSyncPayload payload) {
        if (payload.action() == KamuiChannelSyncPayload.Action.START) {
            beginChannel();
        } else {
            cancelChannel();
        }
    }

    public static boolean isChanneling() {
        return channelActive;
    }

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

    private static boolean isMovementKeyHeld(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        // Check W/A/S/D physical
        boolean w = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean a = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean s = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean d = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean jump = isJumpKeyPhysicallyHeld(mc);
        boolean shift = isShiftKeyPhysicallyHeld(mc);
        boolean sprint = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        return w || a || s || d || jump || shift || sprint;
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!channelActive) return;
        // While channeling: block ALL attack/use interactions. Movement keys are NOT blocked here — they will cancel instead.
        if (event.isAttack() || event.isUseItem() || event.isPickBlock()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!channelActive) return;
        // While channeling you can't do anything — block inventory/container opening.
        // Allow chat screen and our own screens to close, but block inventory.
        if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            event.setCanceled(true);
        } else if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        KamuiIntangibilityStatePayload.tickClientTimer();

        // ── Channel tick (always, even if intangibility inactive) ──
        if (channelActive && minecraft.player != null) {
            // Death also cancels channel (and stops sound)
            if (minecraft.player.isDeadOrDying()) {
                cancelChannel();
                PacketDistributor.sendToServer(new KamuiChannelCancelPayload());
            } else {
                // If player moved beyond threshold -> cancel and notify server
                double dx = minecraft.player.getX() - channelStartX;
                double dy = minecraft.player.getY() - channelStartY;
                double dz = minecraft.player.getZ() - channelStartZ;
                double distSqr = dx * dx + dy * dy + dz * dz;
                boolean moved = distSqr > CHANNEL_MOVEMENT_CANCEL_SQR;

                // Also detect intentional movement key held (more responsive than position, handles freeze attempts)
                // But don't cancel on tiny camera only — need actual key
                // We treat any WASD/jump/shift/sprint as movement intent
                // However, jump/shift already used for intangibility underground; but while channeling intangibility is OFF, so jump/shift = movement
                if (!moved && isMovementKeyHeld(minecraft) && minecraft.screen == null) {
                    // If they are actively holding movement, consider it movement even if position hasn't updated yet this tick
                    // Only cancel if they have been holding for > 1 tick? For instant feedback, cancel immediately.
                    // To avoid accidental micro-taps, we still require position OR key? We'll require key + small delay? For now immediate.
                    // But to avoid cancel on the same tick channel started (where player might already hold W), give 2-tick grace
                    if (channelTicksRemaining < 58) {
                        moved = true;
                    }
                }

                if (moved) {
                    // Movement interrupts channel
                    cancelChannel();
                    PacketDistributor.sendToServer(new KamuiChannelCancelPayload());
                    // Don't return — still handle keybinds below but channel is now cancelled
                } else {
                    // Tick down channel timer
                    channelTicksRemaining--;
                    if (channelTicksRemaining <= 0) {
                        // Channel finished naturally — sound already ended (non-looping 3s) but clear ref so next channel plays every time
                        KamuiSoundManager.stop();
                        channelActive = false;
                        channelTicksRemaining = 0;
                    } else {
                        // While channeling and not moved: freeze is achieved via blocking attack/use above.
                        // We don't forcibly reset position here — we let movement be detected above. If they don't move, they stay.
                        // Optionally clamp delta to zero to prevent drift, but not needed.
                    }
                }
            } // close dead-else
        } else if (channelActive && minecraft.player == null) {
            cancelChannel();
        }

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
            if (minecraft.player == null) continue;
            // If channeling, R should cancel channel and turn on intangibility.
            // We handle sound stop immediately for responsiveness, then let server do force-activate.
            if (channelActive) {
                cancelChannel(); // stop sound locally immediately
                // Still send toggle so server does cancel+forceActivate
                PacketDistributor.sendToServer(new KamuiIntangibilityTogglePayload());
                // Keep GUI closed if somehow open
                if (minecraft.screen != null) {
                    minecraft.setScreen(null);
                }
                continue;
            }
            if (minecraft.screen == null) {
                PacketDistributor.sendToServer(new KamuiIntangibilityTogglePayload());
            }
        }
        while (ModKeybindings.KAMUI_NAVIGATION.consumeClick()) {
            if (minecraft.player == null) continue;
            // While channeling, block navigation wheel open (you can't do anything)
            if (channelActive) {
                // optionally cancel? spec says only R cancels, so just block
                continue;
            }
            if (minecraft.screen == null) {
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