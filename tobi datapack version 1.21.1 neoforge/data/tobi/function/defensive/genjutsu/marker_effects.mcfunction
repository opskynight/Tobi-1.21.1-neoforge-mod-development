# ============================================
# GENJUTSU MARKER EFFECTS (UPDATED - 7x7 RANGE)
# ============================================
# Apply glowing + AI FREEZE to entities within 3.5 blocks of marker (7x7 area)

# Apply glowing to entities within 3.5 blocks of marker (7x7 area)
execute as @e[type=armor_stand,tag=genjutsu_marker] at @s run effect give @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,distance=..3.5] minecraft:glowing 2 0 true

# Tag entities for genjutsu (only if they DON'T have sneak_target tag to prevent interference)
execute as @e[type=armor_stand,tag=genjutsu_marker] at @s run tag @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,tag=!genjutsu_sneak_target,distance=..3.5] add genjutsu_target

# FREEZE AI for all glowing/tagged entities (both raycast AND sneak)
execute as @e[tag=genjutsu_target] run data merge entity @s {NoAI:1b}
execute as @e[tag=genjutsu_sneak_target] run data merge entity @s {NoAI:1b}

# Remove tag, glowing, and RESTORE AI for entities no longer in range of marker
# BUT ONLY if they don't have the sneak_target tag (prevents interference!)
execute as @e[tag=genjutsu_target,tag=!genjutsu_sneak_target] at @s unless entity @e[type=armor_stand,tag=genjutsu_marker,distance=..7] run data merge entity @s {NoAI:0b}
execute as @e[tag=genjutsu_target,tag=!genjutsu_sneak_target] at @s unless entity @e[type=armor_stand,tag=genjutsu_marker,distance=..7] run tag @s remove genjutsu_target
execute as @e[tag=genjutsu_target,tag=!genjutsu_sneak_target] at @s unless entity @e[type=armor_stand,tag=genjutsu_marker,distance=..7] run effect clear @s minecraft:glowing

# Glowing outline particle (7x7 area indicator) - UPDATED range
execute as @e[type=armor_stand,tag=genjutsu_marker] at @s run particle minecraft:witch ~ ~0.5 ~ 3.5 3.5 3.5 0 3 force
