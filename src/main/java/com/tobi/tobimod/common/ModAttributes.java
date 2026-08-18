package com.tobi.tobimod.common;

import com.tobi.tobimod.TobiMod;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public class ModAttributes {

    // Attribute modifier IDs
    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_health");
    private static final ResourceLocation DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_damage");
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_speed");
    private static final ResourceLocation JUMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_jump");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_step_height");
    private static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TobiMod.MOD_ID, "tobi_knockback_resistance");

    public static void applyTobiBuffs(Player player) {
        // +20 HP (10 hearts) - makes total 40 HP
        player.getAttribute(Attributes.MAX_HEALTH).addOrUpdateTransientModifier(
                new AttributeModifier(HEALTH_MODIFIER_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
        );

        // +8 attack damage - makes total 9 with fist
        player.getAttribute(Attributes.ATTACK_DAMAGE).addOrUpdateTransientModifier(
                new AttributeModifier(DAMAGE_MODIFIER_ID, 8.0, AttributeModifier.Operation.ADD_VALUE)
        );

        // +20% movement speed (0.02 added to base 0.10 = 0.12)
        player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
                new AttributeModifier(SPEED_MODIFIER_ID, 0.02, AttributeModifier.Operation.ADD_VALUE)
        );

        // +100% jump strength (double the jump - 0.42 base * 2 = 0.84)
        player.getAttribute(Attributes.JUMP_STRENGTH).addOrUpdateTransientModifier(
                new AttributeModifier(JUMP_MODIFIER_ID, 0.42, AttributeModifier.Operation.ADD_VALUE)
        );

        // 2-block step height (can walk up 2-block obstacles)
        player.getAttribute(Attributes.STEP_HEIGHT).addOrUpdateTransientModifier(
                new AttributeModifier(STEP_HEIGHT_MODIFIER_ID, 1.4, AttributeModifier.Operation.ADD_VALUE)
        );

        // 100% knockback resistance (immune to knockback)
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrUpdateTransientModifier(
                new AttributeModifier(KNOCKBACK_RESISTANCE_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
        );

        // Heal to full health after applying buffs
        player.setHealth(player.getMaxHealth());
    }

    public static void removeTobiBuffs(Player player) {
        player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_ID);
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER_ID);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_ID);
        player.getAttribute(Attributes.JUMP_STRENGTH).removeModifier(JUMP_MODIFIER_ID);
        player.getAttribute(Attributes.STEP_HEIGHT).removeModifier(STEP_HEIGHT_MODIFIER_ID);
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(KNOCKBACK_RESISTANCE_MODIFIER_ID);
    }
}