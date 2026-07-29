# ============================================
# LONG RANGE CHARGE KIDNAP
# ============================================
# Charge for 2 seconds while sneaking, then activate kidnap

# Only charge if: mode 1, has armor, NOT on cooldown, sneaking, marker exists
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=1,tobi_enabled=1,tobi_cooldown=0},predicate=tobi:is_sneaking] at @s if entity @e[type=armor_stand,tag=kamui_marker,distance=..25] run scoreboard players add @s tobi_charge 1

# Reset charge if stopped sneaking
execute as @a[scores={tobi_charge=1..}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_charge 0

# Continuous explosion sound while charging (every 5 ticks = 0.25s)
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 5 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 10 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 15 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 20 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 25 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 30 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5
execute as @a[scores={tobi_charge=1..39}] if score @s tobi_charge matches 35 at @s run playsound minecraft:entity.generic.explode player @s ~ ~ ~ 0.3 1.5

# Action bar feedback
execute as @a[scores={tobi_charge=1..9}] run title @s actionbar {"text":"▰ KAMUI CHARGING ▰","color":"dark_purple","bold":true}
execute as @a[scores={tobi_charge=10..19}] run title @s actionbar {"text":"▰▰ KAMUI CHARGING ▰▰","color":"dark_purple","bold":true}
execute as @a[scores={tobi_charge=20..29}] run title @s actionbar {"text":"▰▰▰ KAMUI CHARGING ▰▰▰","color":"dark_purple","bold":true}
execute as @a[scores={tobi_charge=30..39}] run title @s actionbar {"text":"▰▰▰▰ READY ▰▰▰▰","color":"light_purple","bold":true}

# ACTIVATION at 40 ticks (2 seconds)
execute as @a[scores={tobi_charge=40..}] run function tobi:offensive/long_range/activate
