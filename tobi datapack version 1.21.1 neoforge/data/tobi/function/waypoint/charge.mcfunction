# ============================================
# KAMUI WAYPOINT - CHARGE (FIXED - 3 SECONDS)
# ============================================
# Charge for 3 seconds (60 ticks) to warp or return

# Increase charge if holding waypoint + sneaking + survival + has armor
execute as @a[scores={tobi_waypoint=1,tobi_enabled=1},predicate=tobi:is_sneaking,gamemode=survival] run scoreboard players add @s tobi_charge 1

# Reset charge if they stop sneaking OR no armor OR not holding waypoint
execute as @a[scores={tobi_charge=1..}] unless score @s tobi_waypoint matches 1 run scoreboard players set @s tobi_charge 0
execute as @a[scores={tobi_charge=1..}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_charge 0
execute as @a[scores={tobi_charge=1..}] unless score @s tobi_enabled matches 1 run scoreboard players set @s tobi_charge 0

# Action bar feedback while charging (different based on location)
# Progress bar for charging (60 ticks = 3 seconds)
# 0-12 ticks (0-0.6s)
execute as @a[scores={tobi_waypoint=1,tobi_charge=1..12},predicate=tobi:is_sneaking] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"▰▱▱▱▱ CHARGING WARP ▱▱▱▱▱","color":"dark_purple","bold":true}
execute as @a[scores={tobi_waypoint=1,tobi_charge=1..12},predicate=tobi:is_sneaking,predicate=tobi:in_kamui_void] run title @s actionbar {"text":"▰▱▱▱▱ CHARGING RETURN ▱▱▱▱▱","color":"aqua","bold":true}

# 13-24 ticks (0.6-1.2s)
execute as @a[scores={tobi_waypoint=1,tobi_charge=13..24},predicate=tobi:is_sneaking] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"▰▰▱▱▱ CHARGING WARP ▱▱▱▱▱","color":"dark_purple","bold":true}
execute as @a[scores={tobi_waypoint=1,tobi_charge=13..24},predicate=tobi:is_sneaking,predicate=tobi:in_kamui_void] run title @s actionbar {"text":"▰▰▱▱▱ CHARGING RETURN ▱▱▱▱▱","color":"aqua","bold":true}

# 25-36 ticks (1.2-1.8s)
execute as @a[scores={tobi_waypoint=1,tobi_charge=25..36},predicate=tobi:is_sneaking] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"▰▰▰▱▱ CHARGING WARP ▱▱▱▱▱","color":"dark_purple","bold":true}
execute as @a[scores={tobi_waypoint=1,tobi_charge=25..36},predicate=tobi:is_sneaking,predicate=tobi:in_kamui_void] run title @s actionbar {"text":"▰▰▰▱▱ CHARGING RETURN ▱▱▱▱▱","color":"aqua","bold":true}

# 37-48 ticks (1.8-2.4s)
execute as @a[scores={tobi_waypoint=1,tobi_charge=37..48},predicate=tobi:is_sneaking] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"▰▰▰▰▱ CHARGING WARP ▱▱▱▱▱","color":"light_purple","bold":true}
execute as @a[scores={tobi_waypoint=1,tobi_charge=37..48},predicate=tobi:is_sneaking,predicate=tobi:in_kamui_void] run title @s actionbar {"text":"▰▰▰▰▱ CHARGING RETURN ▱▱▱▱▱","color":"dark_aqua","bold":true}

# 49-59 ticks (2.4-3s)
execute as @a[scores={tobi_waypoint=1,tobi_charge=49..59},predicate=tobi:is_sneaking] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"▰▰▰▰▰ ALMOST READY! ▰▰▰▰▰","color":"gold","bold":true}
execute as @a[scores={tobi_waypoint=1,tobi_charge=49..59},predicate=tobi:is_sneaking,predicate=tobi:in_kamui_void] run title @s actionbar {"text":"▰▰▰▰▰ ALMOST READY! ▰▰▰▰▰","color":"gold","bold":true}

# On 60th tick (3 seconds), activate appropriate function
# If in void → return to waypoint
execute as @a[scores={tobi_charge=60..},predicate=tobi:in_kamui_void] run function tobi:waypoint/activate_return

# If NOT in void → warp to void and save location
execute as @a[scores={tobi_charge=60..}] unless predicate tobi:in_kamui_void run function tobi:waypoint/activate_warp
