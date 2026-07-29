# ============================================
# DISABLE TOBI ABILITIES - FIXED
# ============================================
# Called by toggle_abilities.mcfunction

# Remove all attributes (inline - no function call)
attribute @s minecraft:generic.max_health base set 20
attribute @s minecraft:generic.attack_damage base set 1
attribute @s minecraft:generic.movement_speed base set 0.1
attribute @s minecraft:generic.jump_strength base set 0.42
attribute @s minecraft:generic.fall_damage_multiplier base set 1
attribute @s minecraft:generic.step_height base set 0.6
attribute @s minecraft:generic.burning_time base set 1
attribute @s minecraft:generic.knockback_resistance base set 0

# Clear effects
effect clear @s minecraft:night_vision
effect clear @s minecraft:saturation

# Remove enabled tag
tag @s remove tobi_abilities_enabled

# Remove effects tag
tag @s remove tobi_effects_applied

# CRITICAL: Reset the enabled score to 0
scoreboard players set @s tobi_enabled 0

# Success message
tellraw @s {"text":"[Tobi] Abilities DISABLED!","color":"red","bold":true}
tellraw @s {"text":"→ All buffs removed, COAS abilities locked","color":"gray"}
tellraw @s {"text":"→ Run /function tobi:toggle_abilities again to re-enable","color":"green"}

# Sound effect
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 1 0.5
