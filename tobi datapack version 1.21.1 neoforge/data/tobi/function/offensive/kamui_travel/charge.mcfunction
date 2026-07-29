# ============================================
# KAMUI TRAVEL - CHARGE - DEBUG VERSION
# ============================================
# Charge for 5 seconds (100 ticks) to activate spectator mode

# DEBUG: Show all relevant scores
execute as @a[scores={tobi_offensive_mode=3}] run title @s actionbar [{"text":"Mode:","color":"white"},{"score":{"name":"@s","objective":"tobi_offensive_mode"},"color":"gold"},{"text":" Armor:","color":"white"},{"score":{"name":"@s","objective":"tobi_enabled"},"color":"gold"},{"text":" Charge:","color":"white"},{"score":{"name":"@s","objective":"tobi_kamui_charge"},"color":"aqua"}]

# 1. Increase charge if Mode 3 + Sneaking + Survival + Has Armor
execute as @a[scores={tobi_offensive_mode=3,tobi_enabled=1},predicate=tobi:is_sneaking,gamemode=survival] run scoreboard players add @s tobi_kamui_charge 1

# DEBUG: Announce when charging starts
execute as @a[scores={tobi_offensive_mode=3,tobi_kamui_charge=1}] run tellraw @s {"text":"[DEBUG] Charging started!","color":"green"}

# 2. Reset charge if they stop sneaking OR change modes OR no armor
execute as @a[scores={tobi_kamui_charge=1..}] unless score @s tobi_offensive_mode matches 3 run scoreboard players set @s tobi_kamui_charge 0
execute as @a[scores={tobi_kamui_charge=1..}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_kamui_charge 0
execute as @a[scores={tobi_kamui_charge=1..}] unless entity @s[] run scoreboard players set @s tobi_kamui_charge 0

# 3. Action bar feedback while charging
execute as @a[scores={tobi_offensive_mode=3,tobi_kamui_charge=1..99},predicate=tobi:is_sneaking] run title @s actionbar {"text":"⬤ CHARGING SPECTATOR MODE ⬤","color":"aqua","bold":true}

# 4. On 100th tick (5 seconds), activate spectator mode
execute as @a[scores={tobi_kamui_charge=100..}] run tellraw @s {"text":"[DEBUG] Activating spectator mode!","color":"gold"}
execute as @a[scores={tobi_kamui_charge=100..}] run function tobi:offensive/kamui_travel/activate
