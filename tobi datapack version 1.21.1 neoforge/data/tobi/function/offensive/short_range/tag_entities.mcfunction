# ============================================
# SHORT RANGE - TAG ENTITIES (INSTANT RESPONSE)
# ============================================
# ALWAYS tag entities immediately when conditions are met

# Tag entities within 3 blocks INSTANTLY (no charge requirement)
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=0,tobi_enabled=1}] at @s run tag @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,distance=..3] add short_range_target

# Apply glowing INSTANTLY
execute as @e[tag=short_range_target] run effect give @s minecraft:glowing 1 0 true

# Particle effect around tagged entities
execute as @e[tag=short_range_target] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.5 0.3 0.1 5 force

# Remove tag from entities too far away (>5 blocks)
execute as @e[tag=short_range_target] at @s unless entity @a[scores={tobi_offensive_mode=0},distance=..5] run tag @s remove short_range_target