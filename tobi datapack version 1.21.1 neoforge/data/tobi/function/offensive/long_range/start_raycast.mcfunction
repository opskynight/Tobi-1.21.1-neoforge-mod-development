# ============================================
# START RAYCAST
# ============================================
# Kills old marker and starts new raycast from eye position

# Kill old marker for this player first
execute at @s run kill @e[type=armor_stand,tag=kamui_marker,distance=..30]

# Reset raycast variables
scoreboard players set @s tobi_ray_hit 0
scoreboard players set @s tobi_ray_distance 0

# Start the raycast from eye position
execute positioned ^ ^ ^1 run function tobi:offensive/long_range/raycast_step
