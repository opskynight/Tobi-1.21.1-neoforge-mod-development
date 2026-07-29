# ============================================
# GENJUTSU RAYCAST - STEP
# ============================================
# Performs one step of the raycast (similar to long range kamui)

# Increment distance first
execute as @a[tag=!raycast_done] run scoreboard players add @s tobi_genjutsu_ray_distance 1

# Check for ENTITIES first
execute as @a[tag=!raycast_done] positioned ~-0.5 ~-0.5 ~-0.5 if entity @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,dx=0,limit=1] run tag @s add raycast_hit

# If hit an entity, summon marker HERE
execute as @a[tag=raycast_hit,tag=!raycast_done] run summon armor_stand ~ ~ ~ {Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b,Tags:["genjutsu_marker"],CustomName:'{"text":"Genjutsu Target","color":"red"}'}
execute as @a[tag=raycast_hit] run tag @s add raycast_done
execute as @a[tag=raycast_hit] run tag @s remove raycast_hit

# Check if we hit a solid block
execute unless block ~ ~ ~ #minecraft:air as @a[tag=!raycast_done] run tag @s add raycast_hit

# If hit a block, summon marker at previous position
execute as @a[tag=raycast_hit,tag=!raycast_done] positioned ^ ^ ^-0.5 run summon armor_stand ~ ~ ~ {Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b,Tags:["genjutsu_marker"],CustomName:'{"text":"Genjutsu Target","color":"red"}'}
execute as @a[tag=raycast_hit] run tag @s add raycast_done
execute as @a[tag=raycast_hit] run tag @s remove raycast_hit

# If max distance reached (20 blocks = 40 steps), summon marker here
execute as @a[tag=!raycast_done,scores={tobi_genjutsu_ray_distance=40..}] run summon armor_stand ~ ~ ~ {Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b,Tags:["genjutsu_marker"],CustomName:'{"text":"Genjutsu Target","color":"red"}'}
execute as @a[scores={tobi_genjutsu_ray_distance=40..}] run tag @s add raycast_done

# Continue raycast if not done
execute as @a[tag=!raycast_done,scores={tobi_genjutsu_ray_distance=..39}] positioned ^ ^ ^0.5 run function tobi:defensive/genjutsu/raycast_step

# Cleanup tag
tag @a remove raycast_done
