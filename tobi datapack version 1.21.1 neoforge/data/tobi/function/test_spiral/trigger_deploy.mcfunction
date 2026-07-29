# Reset and start the Deploy animation (State 1)
scoreboard players set @s spiral_state 1
scoreboard players set @s spiral_scale 0
scoreboard players set @s spiral_timer 0

# Replace the armor_stand summon with a Marker for better performance
execute at @s anchored eyes run summon marker ^ ^ ^0.5 {Tags:["kamui_spinner"]}

tellraw @s {"text":"[Spiral] Deploy started!","color":"green"}
playsound minecraft:block.portal.trigger player @s ~ ~ ~ 0.5 1.5