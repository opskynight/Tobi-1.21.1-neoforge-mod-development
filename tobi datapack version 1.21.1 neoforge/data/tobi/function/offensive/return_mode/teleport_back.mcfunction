# ============================================
# RETURN MODE TELEPORT BACK - FIXED
# ============================================
# Modified from return/teleport_back.mcfunction
# Uses offensive_mode=2 instead of slot detection

# CHARGE PHASE: Increment charge score while sneaking in mode 2 (NOT on cooldown)
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=2,tobi_cooldown=0,tobi_enabled=1},predicate=tobi:is_sneaking] run scoreboard players add @s tobi_charge 1

# Reset charge if player stops sneaking or switches mode
execute as @a[scores={tobi_offensive_mode=2}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_charge 0
execute as @a unless score @s tobi_offensive_mode matches 2 run scoreboard players set @s tobi_charge 0

# Ender particles while charging return
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=1..},predicate=tobi:is_sneaking] at @s run particle minecraft:portal ~ ~1 ~ 0.5 0.5 0.5 0.2 10 force

# Apply weakness to nearby entities within 5 blocks while returning
execute as @a[scores={tobi_offensive_mode=2,tobi_cooldown=0},predicate=tobi:is_sneaking] at @s run effect give @e[type=!player,distance=..5] minecraft:weakness 5 255 true

# Action bar feedback showing charge progress
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=1..9},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰ CHARGING RETURN ▰","color":"aqua","bold":true}
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=10..19},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰ CHARGING RETURN ▰▰","color":"aqua","bold":true}
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=20..29},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰ CHARGING RETURN ▰▰▰","color":"aqua","bold":true}
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=30..39},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰▰ ALMOST READY ▰▰▰▰","color":"dark_aqua","bold":true}

# ACTIVATION: When charge reaches 40 - Return all kidnapped entities
execute as @a[scores={tobi_offensive_mode=2,tobi_charge=40..},predicate=tobi:is_sneaking] run function tobi:offensive/return_mode/activate_return
