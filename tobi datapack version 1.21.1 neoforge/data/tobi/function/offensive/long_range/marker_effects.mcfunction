# ============================================
# MARKER EFFECTS
# ============================================
# Apply glowing, tagging, and AI freezing to entities near marker
# 5x5 area = 2.5 block radius from marker

# Apply glowing to entities within 2.5 blocks of marker (5x5 area)
execute as @e[type=armor_stand,tag=kamui_marker] at @s run effect give @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,distance=..2.5] minecraft:glowing 2 0 true

# Tag entities for kidnapping (5x5 area)
execute as @e[type=armor_stand,tag=kamui_marker] at @s run tag @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,distance=..2.5] add kamui_target

# FREEZE entities by removing AI when player is SNEAKING in mode 1
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=1,tobi_enabled=1},predicate=tobi:is_sneaking] at @s as @e[tag=kamui_target,distance=..30] run data merge entity @s {NoAI:1b}

# UNFREEZE entities (restore AI) when player is NOT sneaking
execute as @a[scores={tobi_offensive_mode=1}] unless predicate tobi:is_sneaking at @s as @e[tag=kamui_target,distance=..30,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}

# Remove tag and glowing from entities no longer in range of marker
execute as @e[tag=kamui_target] at @s unless entity @e[type=armor_stand,tag=kamui_marker,distance=..5] run tag @s remove kamui_target
execute as @e[tag=kamui_target] at @s unless entity @e[type=armor_stand,tag=kamui_marker,distance=..5] run effect clear @s minecraft:glowing

# Particle effect at marker location (purple portal)
execute as @e[type=armor_stand,tag=kamui_marker] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.5 0.3 0.1 10 force

# Glowing outline particle (5x5 area indicator)
execute as @e[type=armor_stand,tag=kamui_marker] at @s run particle minecraft:witch ~ ~0.5 ~ 2.5 2.5 2.5 0 5 force
