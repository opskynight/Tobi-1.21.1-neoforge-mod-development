# ============================================
# RETURN MODE COOLDOWN - FIXED
# ============================================
# Modified from return/cooldown.mcfunction

# Increment cooldown timer for players in mode 2
execute as @a[scores={tobi_offensive_mode=2,tobi_cooldown=1..}] run scoreboard players add @s tobi_cooldown 1

# Display cooldown status (120 ticks = 6 seconds total cooldown)
execute as @a[scores={tobi_offensive_mode=2,tobi_cooldown=1..59}] run title @s actionbar {"text":"▱▱▱▱▱ TRANSPORT RECHARGING ▱▱▱▱▱","color":"red","bold":true}
execute as @a[scores={tobi_offensive_mode=2,tobi_cooldown=60..119}] run title @s actionbar {"text":"▱▱▱▱▱▱▱▱▱▱ ALMOST READY ▱▱▱▱▱▱▱▱▱▱","color":"yellow","bold":true}

# Reset cooldown after 120 ticks (6 seconds)
execute as @a[scores={tobi_cooldown=120..}] run scoreboard players set @s tobi_cooldown 0

# Ready notification - show when not charging
execute as @a[scores={tobi_offensive_mode=2,tobi_cooldown=0,tobi_enabled=1}] unless score @s tobi_charge matches 1.. run title @s actionbar {"text":"✦ RETURN READY - SNEAK TO RECALL ✦","color":"green","bold":true}
