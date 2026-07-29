# ============================================
# APPLY ATTRIBUTES WHEN ABILITIES ENABLED
# ============================================

# Health (+20 HP = 40 total)
attribute @s minecraft:generic.max_health base set 40

# Attack Damage (+8 = 9 total with fist)
attribute @s minecraft:generic.attack_damage base set 9

# Movement Speed (+20% = 0.12 speed)
attribute @s minecraft:generic.movement_speed base set 0.12

# Jump Strength (+100% = 0.84 jump)
attribute @s minecraft:generic.jump_strength base set 0.84

# Fall Damage Immunity (0x multiplier = no fall damage)
attribute @s minecraft:generic.fall_damage_multiplier base set 0

# Step Height (can walk up 2-block tall steps)
attribute @s minecraft:generic.step_height base set 2

# Fire Immunity (0 burning time = instant extinguish)
attribute @s minecraft:generic.burning_time base set 0

# Knockback Resistance (100% immunity to knockback)
attribute @s minecraft:generic.knockback_resistance base set 1.0

# Mark player as having attributes applied
tag @s add tobi_attributes_applied

# Feedback message
tellraw @s {"text":"[Tobi] Attributes applied! (40 HP, knockback immunity, no fall/fire damage, +2 step height)","color":"gold","bold":true}