# ============================================
# TOBI DATAPACK - UNINSTALL (UPDATED WITH GENJUTSU)
# ============================================

# Remove all scoreboard objectives
scoreboard objectives remove tobi_slot
# Remove ability tags
tag @a remove tobi_abilities_enabled
tag @a remove tobi_attributes_applied
tag @a remove tobi_effects_applied
scoreboard objectives remove tobi_death

# COAS Detection
scoreboard objectives remove tobi_defensive
scoreboard objectives remove tobi_offensive
scoreboard objectives remove tobi_dimensional
scoreboard objectives remove tobi_waypoint
scoreboard objectives remove tobi_offhand_swap

# Defensive Style
scoreboard objectives remove tobi_defensive_mode
scoreboard objectives remove tobi_phase
scoreboard objectives remove tobi_underground

# Genjutsu (now in Defensive)
scoreboard objectives remove tobi_genjutsu_ray_distance
scoreboard objectives remove tobi_genjutsu_ray_hit
scoreboard objectives remove tobi_genjutsu_sneak_freeze
scoreboard objectives remove tobi_charge
scoreboard objectives remove tobi_used_coas
scoreboard objectives remove tobi_genjutsu_dmg
scoreboard objectives remove tobi_genjutsu_timer

# Offensive Style
scoreboard objectives remove tobi_offensive_mode
scoreboard objectives remove tobi_charge
scoreboard objectives remove tobi_cooldown
scoreboard objectives remove tobi_ray_distance
scoreboard objectives remove tobi_ray_hit
scoreboard objectives remove tobi_charge
scoreboard objectives remove tobi_cooldown
scoreboard objectives remove tobi_cooldown
scoreboard objectives remove tobi_charge
scoreboard objectives remove tobi_entity_marked
scoreboard objectives remove tobi_maintain_timer
scoreboard objectives remove tobi_temp_x
scoreboard objectives remove tobi_temp_health

# Dimensional Style (Genjutsu removed)
scoreboard objectives remove tobi_dimensional_mode
scoreboard objectives remove tobi_kamui_charge
scoreboard objectives remove tobi_kamui_active
scoreboard objectives remove tobi_kamui_pos_x
scoreboard objectives remove tobi_kamui_pos_y
scoreboard objectives remove tobi_kamui_pos_z
scoreboard objectives remove tobi_kamui_stillness
scoreboard objectives remove tobi_temp_y
scoreboard objectives remove tobi_temp_z
scoreboard objectives remove tobi_charge
scoreboard objectives remove tobi_return_x
scoreboard objectives remove tobi_return_y
scoreboard objectives remove tobi_return_z
scoreboard objectives remove tobi_return_dim

# Waypoint System
scoreboard objectives remove tobi_charge

# Extra Features
scoreboard objectives remove spiral_state
scoreboard objectives remove spiral_scale
scoreboard objectives remove spiral_timer
scoreboard objectives remove spiral_rotation
scoreboard objectives remove tobi_barrier_timer

# Clean up entities and effects
kill @e[type=armor_stand,tag=kamui_marker]
kill @e[type=armor_stand,tag=genjutsu_marker]
kill @e[type=marker,tag=kamui_spinner]
gamemode survival @a[gamemode=spectator]
effect clear @e

# Restore AI for any frozen entities
execute as @e[type=!player,type=!item,type=!experience_orb,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}

# Remove all tags
tag @e[tag=genjutsu_damaged] remove genjutsu_damaged
tag @e[tag=genjutsu_target] remove genjutsu_target
tag @e[tag=genjutsu_sneak_target] remove genjutsu_sneak_target
tag @e[tag=genjutsu_execute] remove genjutsu_execute
tag @e[tag=tobi_kidnapped] remove tobi_kidnapped
tag @e[tag=short_range_target] remove short_range_target
tag @e[tag=kamui_target] remove kamui_target

# Remove player tags
tag @a remove tobi_attributes_applied
tag @a remove tobi_effects_applied
tag @a remove tobi_armor_notified
tag @a remove in_kamui_void
tag @a remove in_void_mode

tellraw @a {"text":"[Tobi] Complete COAS System uninstalled (including Genjutsu).","color":"red"}
