# ============================================
# PHASING CHECK UNDERGROUND
# ============================================
# Switch to spectator when head enters a solid block (while phasing in survival)

execute as @a[gamemode=survival,scores={tobi_phase=1,tobi_kamui_active=0}] at @s unless block ~ ~1.8 ~ #minecraft:air run gamemode spectator @s
execute as @a[gamemode=survival,scores={tobi_phase=1,tobi_kamui_active=0}] at @s unless block ~ ~1.8 ~ #minecraft:air run tellraw @s {"text":"[Phasing] Underground mode activated.","color":"gray"}
execute as @a[gamemode=survival,scores={tobi_phase=1,tobi_kamui_active=0}] at @s unless block ~ ~1.8 ~ #minecraft:air run playsound minecraft:block.stone.break player @s ~ ~ ~ 0.5 0.8
