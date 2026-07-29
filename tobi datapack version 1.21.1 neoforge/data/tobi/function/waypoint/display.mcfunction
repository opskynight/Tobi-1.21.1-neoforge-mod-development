# ============================================
# KAMUI WAYPOINT - DISPLAY
# ============================================
# Shows current status in action bar

# No armor warning (overrides everything)
execute as @a[scores={tobi_waypoint=1,tobi_enabled=0}] run title @s actionbar {"text":"⚠ ABILITIES DISABLED ⚠","color":"red","bold":true}

# In void, ready to return (not charging)
execute as @a[scores={tobi_waypoint=1,tobi_charge=0,tobi_enabled=1},predicate=tobi:in_kamui_void] run title @s actionbar {"text":"✦ WAYPOINT READY - SNEAK 3s TO RETURN ✦","color":"aqua","bold":true}

# Not in void, ready to warp (not charging)
execute as @a[scores={tobi_waypoint=1,tobi_charge=0,tobi_enabled=1}] unless predicate tobi:in_kamui_void run title @s actionbar {"text":"✦ WAYPOINT READY - SNEAK 3s TO WARP ✦","color":"dark_purple","bold":true}
