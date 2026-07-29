# ============================================
# KAMUI WAYPOINT - ACTIVATE RETURN (FIXED)
# ============================================
# Return to stored waypoint location

# Get player's UUID
data modify storage tobi:temp uuid set from entity @s UUID

# Debug: Show what's in storage
tellraw @s [{"text":"[DEBUG] Storage contents: ","color":"yellow"},{"nbt":"waypoint.locations[-1]","storage":"tobi:"}]

# CRITICAL FIX: Copy the stored location data to temp for macro use
data modify storage tobi:temp return_data set from storage tobi:waypoint locations[-1]

# Debug: Show what we're about to use
tellraw @s [{"text":"[DEBUG] Using return_data: ","color":"aqua"},{"nbt":"temp.return_data","storage":"tobi:"}]

# Teleport using macro function
function tobi:waypoint/teleport_to_stored with storage tobi:temp return_data

# Clear the stored location (single-use waypoint)
data remove storage tobi:waypoint locations[-1]

# Reset charge
scoreboard players set @s tobi_charge 0

# Messages and effects
tellraw @s {"text":"[Kamui Waypoint] Returned to waypoint!","color":"green","bold":true}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 2
execute at @s run particle minecraft:portal ~ ~1 ~ 1 1 1 1 100 force

# Update COAS back to normal "Waypoint" mode
function tobi:waypoint/update_coas_waypoint
