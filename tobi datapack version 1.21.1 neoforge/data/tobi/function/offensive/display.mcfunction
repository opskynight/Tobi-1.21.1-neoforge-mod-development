# ============================================
# OFFENSIVE STYLE STATUS DISPLAY - FIXED
# ============================================
# Shows current mode and status (only when not charging/cooling down)

# No armor warning (overrides everything)
execute as @a[scores={tobi_offensive=1,tobi_enabled=0}] run title @s actionbar {"text":"⚠ ABILITIES DISABLED ⚠","color":"red","bold":true}

# Mode displays (only shown when not charging/cooldown/no armor)
# Short Range - Idle
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=0,tobi_enabled=1}] unless predicate tobi:is_sneaking run title @s actionbar {"text":"◈ OFFENSIVE: SHORT RANGE ◈","color":"red","bold":true}

# Short Range - Active
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=0,tobi_enabled=1},predicate=tobi:is_sneaking] run title @s actionbar {"text":"◈◈◈ FREEZING NEARBY ENTITIES ◈◈◈","color":"dark_red","bold":true}

# Long Range - handled by cooldown.mcfunction

# Return - handled by cooldown.mcfunction

# ============================================
# CRITICAL FIX: Travel display should check tobi_offensive_mode, not tobi_offensive
# This way it still shows even if tobi_offensive briefly drops to 0
# ============================================
# Travel - Idle (not charging, not in spectator)
execute as @a[scores={tobi_offensive_mode=3,tobi_enabled=1,tobi_kamui_charge=0,tobi_kamui_active=0}] run title @s actionbar {"text":"◈ TRAVEL: SNEAK 5s for Spectator ◈","color":"aqua","bold":true}

# Travel - In spectator (handled by detect_stillness progress bar)
