# ============================================
# SHORT RANGE - CHARGE KIDNAP (SYNTAX FIXED!)
# ============================================
# Charge for 2 seconds (40 ticks), then kidnap nearby entities

# Only charge if: mode 0, has armor, NOT on cooldown, sneaking
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=0,tobi_enabled=1,tobi_cooldown=0},predicate=tobi:is_sneaking] run scoreboard players add @s tobi_charge 1

# Reset charge if stopped sneaking (FIXED SYNTAX ERROR HERE!)
execute as @a[scores={tobi_charge=1..}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_charge 0

# FREEZE entities by removing AI while charging (ONLY when sneaking AND charging)
execute as @a[scores={tobi_offensive_mode=0,tobi_charge=1..},predicate=tobi:is_sneaking] at @s as @e[tag=short_range_target,distance=..5] run data merge entity @s {NoAI:1b}

# UNFREEZE entities (restore AI) when player stops sneaking or charge resets
execute as @a[scores={tobi_offensive_mode=0}] unless predicate tobi:is_sneaking at @s as @e[tag=short_range_target,distance=..5,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}
execute as @a[scores={tobi_offensive_mode=0,tobi_charge=0}] at @s as @e[tag=short_range_target,distance=..5,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}

# Continuous explosion sound while charging (every 5 ticks = 0.25s)
execute as @a[scores={tobi_charge=5}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=10}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=15}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=20}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=25}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=30}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=35}] at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5

# Action bar feedback
execute as @a[scores={tobi_charge=1..9}] run title @s actionbar {"text":"▰ KAMUI CHARGING ▰","color":"dark_red","bold":true}
execute as @a[scores={tobi_charge=10..19}] run title @s actionbar {"text":"▰▰ KAMUI CHARGING ▰▰","color":"dark_red","bold":true}
execute as @a[scores={tobi_charge=20..29}] run title @s actionbar {"text":"▰▰▰ KAMUI CHARGING ▰▰▰","color":"dark_red","bold":true}
execute as @a[scores={tobi_charge=30..39}] run title @s actionbar {"text":"▰▰▰▰ READY ▰▰▰▰","color":"red","bold":true}

# ACTIVATION at 40 ticks (2 seconds)
execute as @a[scores={tobi_charge=40..}] run function tobi:offensive/short_range/activate
