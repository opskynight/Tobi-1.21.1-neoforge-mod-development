# ============================================
# RETURN MODE DETECT
# ============================================
# Modified from return/detect.mcfunction
# Uses offensive_mode=2 instead of slot detection

# Reset cooldown if player is NOT on offensive mode 2
execute as @a unless score @s tobi_offensive_mode matches 2 run scoreboard players set @s tobi_cooldown 0

# Reset charge if player is NOT on offensive mode 2
execute as @a unless score @s tobi_offensive_mode matches 2 run scoreboard players set @s tobi_charge 0
