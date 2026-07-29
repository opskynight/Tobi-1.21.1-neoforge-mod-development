# ============================================
# GENJUTSU RAYCAST CONTROL
# ============================================
# Only update marker position every 5 ticks to reduce lag

# Only update marker position every 5 ticks
scoreboard players add @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_enabled=1}] tobi_genjutsu_ray_distance 1

# Run raycast every 5 ticks
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_enabled=1,tobi_genjutsu_ray_distance=5..}] at @s anchored eyes run function tobi:defensive/genjutsu/raycast_start
