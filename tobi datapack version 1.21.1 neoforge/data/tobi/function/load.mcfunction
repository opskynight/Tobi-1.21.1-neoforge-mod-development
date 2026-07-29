# ============================================
# TOBI DATAPACK - LOAD (COAS SYSTEM) - FIXED
# ============================================

# Core Logic
scoreboard objectives add tobi_slot dummy
scoreboard objectives add tobi_death deathCount

# CRITICAL: Add enabled score (manual toggle)
scoreboard objectives add tobi_enabled dummy "Abilities enabled flag"

# ============================================
# COAS DETECTION SCORES
# ============================================
scoreboard objectives add tobi_defensive dummy "Holding Defensive COAS"
scoreboard objectives add tobi_offensive dummy "Holding Offensive COAS"
scoreboard objectives add tobi_dimensional dummy "Holding Dimensional COAS"
scoreboard objectives add tobi_waypoint dummy "Holding Waypoint COAS"

# Mode switching detection
scoreboard objectives add tobi_offhand_swap dummy "Offhand swap detection"

# ============================================
# DEFENSIVE STYLE (Invulnerability + Genjutsu)
# ============================================
scoreboard objectives add tobi_defensive_mode dummy "0=Invulnerability, 1=Genjutsu"
scoreboard objectives add tobi_phase dummy
scoreboard objectives add tobi_underground dummy

# Genjutsu raycast
scoreboard objectives add tobi_genjutsu_ray_distance dummy "Genjutsu raycast distance"
scoreboard objectives add tobi_genjutsu_ray_hit dummy "Genjutsu raycast hit"

# Genjutsu sneak freeze
scoreboard objectives add tobi_genjutsu_sneak_freeze dummy "Sneak freeze state"
scoreboard objectives add tobi_genjutsu_sneak_timer dummy "Sneak freeze timer (DEDICATED)"
scoreboard objectives add tobi_charge dummy "Universal charge timer"

# Genjutsu right click detection
scoreboard objectives add tobi_used_coas minecraft.used:minecraft.carrot_on_a_stick "Right click detection"

# ============================================
# WAYPOINT CREATOR SYSTEM
# ============================================
scoreboard objectives add tobi_used_compass minecraft.used:minecraft.compass "Compass right-click detection"

# ============================================
# OFFENSIVE STYLE (Short/Long/Return/Travel)
# ============================================
scoreboard objectives add tobi_offensive_mode dummy "0=Short, 1=Long, 2=Return, 3=Travel"

# Universal cooldown
scoreboard objectives add tobi_cooldown dummy "Universal cooldown"

# Raycast
scoreboard objectives add tobi_ray_distance dummy
scoreboard objectives add tobi_ray_hit dummy

# Entity tracking
scoreboard objectives add tobi_entity_marked dummy
scoreboard objectives add tobi_maintain_timer dummy

# Temporary storage for counts/calculations
scoreboard objectives add tobi_temp_x dummy
scoreboard objectives add tobi_temp_health dummy

# ============================================
# DIMENSIONAL STYLE (Travel/Dimension)
# ============================================
scoreboard objectives add tobi_dimensional_mode dummy "0=Travel, 1=Dimension"

# Travel Mode (Mode 0)
scoreboard objectives add tobi_kamui_charge dummy "Kamui travel charge"
scoreboard objectives add tobi_kamui_active dummy "Kamui travel active"
scoreboard objectives add tobi_kamui_pos_x dummy "Kamui position X"
scoreboard objectives add tobi_kamui_pos_y dummy "Kamui position Y"
scoreboard objectives add tobi_kamui_pos_z dummy "Kamui position Z"
scoreboard objectives add tobi_kamui_stillness dummy "Kamui stillness timer"
scoreboard objectives add tobi_temp_y dummy "Temp Y storage"
scoreboard objectives add tobi_temp_z dummy "Temp Z storage"

# Dimension Mode (Mode 1)
scoreboard objectives add tobi_return_x dummy "Return X coordinate"
scoreboard objectives add tobi_return_y dummy "Return Y coordinate"
scoreboard objectives add tobi_return_z dummy "Return Z coordinate"
scoreboard objectives add tobi_return_dim dummy "Return dimension ID"

# Genjutsu damage calculation (used by Defensive)
scoreboard objectives add tobi_genjutsu_dmg dummy "Genjutsu damage calc"
scoreboard objectives add tobi_genjutsu_timer dummy "Genjutsu damage cooldown"

# Set constants for genjutsu
scoreboard players set #3 tobi_genjutsu_dmg 3
scoreboard players set #100 tobi_genjutsu_dmg 100
scoreboard players set #20 tobi_genjutsu_dmg 20

# ============================================
# EXTRA FEATURES
# ============================================
# Spiral Animation (Slot 6)
scoreboard objectives add spiral_state dummy
scoreboard objectives add spiral_scale dummy
scoreboard objectives add spiral_timer dummy
scoreboard objectives add spiral_rotation dummy

# Barrier Timer
scoreboard objectives add tobi_barrier_timer dummy

# ============================================
# INITIALIZE ALL PLAYER SCORES TO 0
# ============================================

# Core scores - INITIALIZE TO 0
execute as @a unless score @s tobi_enabled = @s tobi_enabled run scoreboard players set @s tobi_enabled 0
execute as @a unless score @s tobi_defensive = @s tobi_defensive run scoreboard players set @s tobi_defensive 0
execute as @a unless score @s tobi_offensive = @s tobi_offensive run scoreboard players set @s tobi_offensive 0
execute as @a unless score @s tobi_dimensional = @s tobi_dimensional run scoreboard players set @s tobi_dimensional 0
execute as @a unless score @s tobi_waypoint = @s tobi_waypoint run scoreboard players set @s tobi_waypoint 0

# Defensive scores
execute as @a unless score @s tobi_defensive_mode = @s tobi_defensive_mode run scoreboard players set @s tobi_defensive_mode 0
execute as @a unless score @s tobi_phase = @s tobi_phase run scoreboard players set @s tobi_phase 0
execute as @a unless score @s tobi_underground = @s tobi_underground run scoreboard players set @s tobi_underground 0

# Genjutsu scores
execute as @a unless score @s tobi_genjutsu_ray_distance = @s tobi_genjutsu_ray_distance run scoreboard players set @s tobi_genjutsu_ray_distance 0
execute as @a unless score @s tobi_genjutsu_ray_hit = @s tobi_genjutsu_ray_hit run scoreboard players set @s tobi_genjutsu_ray_hit 0
execute as @a unless score @s tobi_genjutsu_sneak_freeze = @s tobi_genjutsu_sneak_freeze run scoreboard players set @s tobi_genjutsu_sneak_freeze 0
execute as @a unless score @s tobi_genjutsu_sneak_timer = @s tobi_genjutsu_sneak_timer run scoreboard players set @s tobi_genjutsu_sneak_timer 0
execute as @a unless score @s tobi_charge = @s tobi_charge run scoreboard players set @s tobi_charge 0

# Offensive scores
execute as @a unless score @s tobi_offensive_mode = @s tobi_offensive_mode run scoreboard players set @s tobi_offensive_mode 0
execute as @a unless score @s tobi_cooldown = @s tobi_cooldown run scoreboard players set @s tobi_cooldown 0
execute as @a unless score @s tobi_ray_distance = @s tobi_ray_distance run scoreboard players set @s tobi_ray_distance 0

# Dimensional scores
execute as @a unless score @s tobi_dimensional_mode = @s tobi_dimensional_mode run scoreboard players set @s tobi_dimensional_mode 0
execute as @a unless score @s tobi_kamui_charge = @s tobi_kamui_charge run scoreboard players set @s tobi_kamui_charge 0
execute as @a unless score @s tobi_kamui_active = @s tobi_kamui_active run scoreboard players set @s tobi_kamui_active 0
execute as @a unless score @s tobi_kamui_stillness = @s tobi_kamui_stillness run scoreboard players set @s tobi_kamui_stillness 0

# Spiral scores
execute as @a unless score @s spiral_state = @s spiral_state run scoreboard players set @s spiral_state 0
execute as @a unless score @s spiral_scale = @s spiral_scale run scoreboard players set @s spiral_scale 0
execute as @a unless score @s spiral_timer = @s spiral_timer run scoreboard players set @s spiral_timer 0
execute as @a unless score @s spiral_rotation = @s spiral_rotation run scoreboard players set @s spiral_rotation 0

# Barrier timer
execute as @a unless score @s tobi_barrier_timer = @s tobi_barrier_timer run scoreboard players set @s tobi_barrier_timer 0

# Temp scores
execute as @a unless score @s tobi_temp_x = @s tobi_temp_x run scoreboard players set @s tobi_temp_x 0
execute as @a unless score @s tobi_temp_y = @s tobi_temp_y run scoreboard players set @s tobi_temp_y 0
execute as @a unless score @s tobi_temp_z = @s tobi_temp_z run scoreboard players set @s tobi_temp_z 0
execute as @a unless score @s tobi_temp_health = @s tobi_temp_health run scoreboard players set @s tobi_temp_health 0

tellraw @a {"text":"[Tobi] COAS System Loaded! (With Waypoint Creator)","color":"gold","bold":true}
tellraw @a {"text":"→ Use /function tobi:toggle_abilities to enable/disable powers","color":"yellow"}
tellraw @a {"text":"→ /function tobi:waypoint_creator/give_creator for Waypoint Creator","color":"green"}
