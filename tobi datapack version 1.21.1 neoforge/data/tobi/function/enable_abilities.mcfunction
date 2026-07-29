# ============================================
# ENABLE TOBI ABILITIES - FIXED
# ============================================
# Called by toggle_abilities.mcfunction

# Apply all attributes (inline - no function call)
attribute @s minecraft:generic.max_health base set 40
attribute @s minecraft:generic.attack_damage base set 9
attribute @s minecraft:generic.movement_speed base set 0.12
attribute @s minecraft:generic.jump_strength base set 0.84
attribute @s minecraft:generic.fall_damage_multiplier base set 0
attribute @s minecraft:generic.step_height base set 2
attribute @s minecraft:generic.burning_time base set 0
attribute @s minecraft:generic.knockback_resistance base set 1.0

# Apply infinite effects
effect give @s minecraft:night_vision infinite 0 true
effect give @s minecraft:saturation infinite 0 true

# Set enabled tag
tag @s add tobi_abilities_enabled

# Mark as having effects applied
tag @s add tobi_effects_applied

# CRITICAL: Set the enabled score to 1
scoreboard players set @s tobi_enabled 1

# Success message
tellraw @s {"text":"[Tobi] Abilities ENABLED!","color":"gold","bold":true}
tellraw @s {"text":"→ All buffs active, all COAS abilities unlocked","color":"yellow"}
tellraw @s {"text":"→ Run /function tobi:toggle_abilities again to disable","color":"green"}

# Sound effect
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 1 1
