# ============================================
# LONG RANGE RAYCAST CONTROL - FIXED
# ============================================
# Updates marker position every 5 ticks to reduce lag

# Only update marker position every 5 ticks
# REMOVED tobi_charge=0 condition so raycast works while charging!
scoreboard players add @a[scores={tobi_offensive=1,tobi_offensive_mode=1,tobi_enabled=1,tobi_cooldown=0}] tobi_ray_distance 1

# Run raycast every 5 ticks
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=1,tobi_enabled=1,tobi_cooldown=0,tobi_ray_distance=5..}] at @s anchored eyes run function tobi:offensive/long_range/start_raycast
