# Reset and start the Withdraw animation (State 2)
scoreboard players set @s spiral_state 2
scoreboard players set @s spiral_scale 100
scoreboard players set @s spiral_timer 0

tellraw @s {"text":"[Spiral] Withdraw started!","color":"yellow"}
playsound minecraft:block.portal.trigger player @s ~ ~ ~ 0.5 0.8