# ============================================
# RESET ABILITIES - MANUAL FIX COMMAND
# ============================================
# Usage: /function tobi:reset_abilities
# Use when:
#  - Starting a new world
#  - Abilities glitching after code changes
#  - Scores in invalid states during testing
#
# This initializes/resets ALL scores to default values

# Core scores
execute unless score @s tobi_enabled = @s tobi_enabled run scoreboard players set @s tobi_enabled 0
execute unless score @s tobi_defensive = @s tobi_defensive run scoreboard players set @s tobi_defensive 0
execute unless score @s tobi_offensive = @s tobi_offensive run scoreboard players set @s tobi_offensive 0
execute unless score @s tobi_dimensional = @s tobi_dimensional run scoreboard players set @s tobi_dimensional 0
execute unless score @s tobi_waypoint = @s tobi_waypoint run scoreboard players set @s tobi_waypoint 0
execute unless score @s tobi_offhand_swap = @s tobi_offhand_swap run scoreboard players set @s tobi_offhand_swap 0

# Defensive scores
execute unless score @s tobi_defensive_mode = @s tobi_defensive_mode run scoreboard players set @s tobi_defensive_mode 0
execute unless score @s tobi_phase = @s tobi_phase run scoreboard players set @s tobi_phase 0
execute unless score @s tobi_underground = @s tobi_underground run scoreboard players set @s tobi_underground 0

# Genjutsu scores (now in Defensive)
execute unless score @s tobi_genjutsu_ray_distance = @s tobi_genjutsu_ray_distance run scoreboard players set @s tobi_genjutsu_ray_distance 0
execute unless score @s tobi_genjutsu_ray_hit = @s tobi_genjutsu_ray_hit run scoreboard players set @s tobi_genjutsu_ray_hit 0
execute unless score @s tobi_genjutsu_sneak_freeze = @s tobi_genjutsu_sneak_freeze run scoreboard players set @s tobi_genjutsu_sneak_freeze 0
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0
execute unless score @s tobi_genjutsu_dmg = @s tobi_genjutsu_dmg run scoreboard players set @s tobi_genjutsu_dmg 0
execute unless score @s tobi_genjutsu_timer = @s tobi_genjutsu_timer run scoreboard players set @s tobi_genjutsu_timer 0

# Offensive scores
execute unless score @s tobi_offensive_mode = @s tobi_offensive_mode run scoreboard players set @s tobi_offensive_mode 0
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0
execute unless score @s tobi_cooldown = @s tobi_cooldown run scoreboard players set @s tobi_cooldown 0
execute unless score @s tobi_ray_distance = @s tobi_ray_distance run scoreboard players set @s tobi_ray_distance 0
execute unless score @s tobi_ray_hit = @s tobi_ray_hit run scoreboard players set @s tobi_ray_hit 0
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0
execute unless score @s tobi_cooldown = @s tobi_cooldown run scoreboard players set @s tobi_cooldown 0
execute unless score @s tobi_cooldown = @s tobi_cooldown run scoreboard players set @s tobi_cooldown 0
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0

# Dimensional scores
execute unless score @s tobi_dimensional_mode = @s tobi_dimensional_mode run scoreboard players set @s tobi_dimensional_mode 0
execute unless score @s tobi_kamui_charge = @s tobi_kamui_charge run scoreboard players set @s tobi_kamui_charge 0
execute unless score @s tobi_kamui_active = @s tobi_kamui_active run scoreboard players set @s tobi_kamui_active 0
execute unless score @s tobi_kamui_pos_x = @s tobi_kamui_pos_x run scoreboard players set @s tobi_kamui_pos_x 0
execute unless score @s tobi_kamui_pos_y = @s tobi_kamui_pos_y run scoreboard players set @s tobi_kamui_pos_y 0
execute unless score @s tobi_kamui_pos_z = @s tobi_kamui_pos_z run scoreboard players set @s tobi_kamui_pos_z 0
execute unless score @s tobi_kamui_stillness = @s tobi_kamui_stillness run scoreboard players set @s tobi_kamui_stillness 0
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0
execute unless score @s tobi_return_x = @s tobi_return_x run scoreboard players set @s tobi_return_x 0
execute unless score @s tobi_return_y = @s tobi_return_y run scoreboard players set @s tobi_return_y 0
execute unless score @s tobi_return_z = @s tobi_return_z run scoreboard players set @s tobi_return_z 0
execute unless score @s tobi_return_dim = @s tobi_return_dim run scoreboard players set @s tobi_return_dim 0

# Waypoint scores
execute unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0

# Spiral scores
execute unless score @s spiral_state = @s spiral_state run scoreboard players set @s spiral_state 0
execute unless score @s spiral_scale = @s spiral_scale run scoreboard players set @s spiral_scale 0
execute unless score @s spiral_timer = @s spiral_timer run scoreboard players set @s spiral_timer 0
execute unless score @s spiral_rotation = @s spiral_rotation run scoreboard players set @s spiral_rotation 0

# Barrier timer
execute unless score @s tobi_barrier_timer = @s tobi_barrier_timer run scoreboard players set @s tobi_barrier_timer 0

# Temp scores
execute unless score @s tobi_temp_x = @s tobi_temp_x run scoreboard players set @s tobi_temp_x 0
execute unless score @s tobi_temp_y = @s tobi_temp_y run scoreboard players set @s tobi_temp_y 0
execute unless score @s tobi_temp_z = @s tobi_temp_z run scoreboard players set @s tobi_temp_z 0
execute unless score @s tobi_temp_health = @s tobi_temp_health run scoreboard players set @s tobi_temp_health 0

# Welcome message
tellraw @s {"text":"[Tobi] Player scores initialized! (No armor required)","color":"green"}
tellraw @s {"text":"→ Use /function tobi:toggle to enable your powers!","color":"yellow"}
