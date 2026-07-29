# ============================================
# GENJUTSU RAYCAST - START
# ============================================
# Starts raycast from eye position for Genjutsu targeting

# Kill old marker for this player first
execute at @s run kill @e[type=armor_stand,tag=genjutsu_marker,distance=..25]

# Reset raycast variables
scoreboard players set @s tobi_genjutsu_ray_hit 0
scoreboard players set @s tobi_genjutsu_ray_distance 0

# Start the raycast from eye position
execute positioned ^ ^ ^1 run function tobi:defensive/genjutsu/raycast_step
