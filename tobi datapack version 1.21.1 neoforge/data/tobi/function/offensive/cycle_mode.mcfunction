# ============================================
# CYCLE OFFENSIVE MODE - FIXED
# ============================================
# Changes the COAS item to the next mode
# Short (0) → Long (1) → Return (2) → Travel (3) → Short (0)

# Mark that we've processed this swap
scoreboard players set @s tobi_offhand_swap 1

# ============================================
# CRITICAL FIX: Clear BOTH hands before giving new item
# ============================================
clear @s carrot_on_a_stick[custom_data~{tobi_offensive:1b}]

# ============================================
# SHORT RANGE (0) → LONG RANGE (1)
# ============================================
execute if score @s tobi_offensive_mode matches 0 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Offensive (Long Range)","color":"dark_purple","bold":true,"italic":false}',lore=['{"text":"Tobi\'s offensive capabilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Short → Long → Return → Travel","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"LONG RANGE MODE:","color":"light_purple","italic":false}','{"text":"→ AIM: Place target marker","color":"dark_purple","italic":false}','{"text":"→ SNEAK 2s: Kidnap to void","color":"dark_purple","italic":false}'],custom_model_data=2,custom_data={tobi_offensive:1b,offensive_mode:1}] 1
execute if score @s tobi_offensive_mode matches 0 run tellraw @s {"text":"[Offensive] Switched to LONG RANGE mode","color":"light_purple","bold":true}
execute if score @s tobi_offensive_mode matches 0 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1.5

# ============================================
# LONG RANGE (1) → RETURN (2)
# ============================================
execute if score @s tobi_offensive_mode matches 1 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Offensive (Return)","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Tobi\'s offensive capabilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Short → Long → Return → Travel","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RETURN MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 2s: Return entities from void","color":"dark_aqua","italic":false}'],custom_model_data=2,custom_data={tobi_offensive:1b,offensive_mode:2}] 1
execute if score @s tobi_offensive_mode matches 1 run tellraw @s {"text":"[Offensive] Switched to RETURN mode","color":"aqua","bold":true}
execute if score @s tobi_offensive_mode matches 1 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1.2

# ============================================
# RETURN (2) → KAMUI TRAVEL (3)
# ============================================
execute if score @s tobi_offensive_mode matches 2 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Offensive (Travel)","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Tobi\'s offensive capabilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Short → Long → Return → Travel","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"TRAVEL MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Spectator mode","color":"dark_aqua","italic":false}','{"text":"→ Stand still 3s: Return to survival","color":"dark_aqua","italic":false}'],custom_model_data=2,custom_data={tobi_offensive:1b,offensive_mode:3}] 1
execute if score @s tobi_offensive_mode matches 2 run tellraw @s {"text":"[Offensive] Switched to TRAVEL mode","color":"aqua","bold":true}
execute if score @s tobi_offensive_mode matches 2 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1.0

# ============================================
# KAMUI TRAVEL (3) → SHORT RANGE (0)
# ============================================
execute if score @s tobi_offensive_mode matches 3 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Offensive (Short Range)","color":"dark_purple","bold":true,"italic":false}',lore=['{"text":"Tobi\'s offensive capabilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Short → Long → Return → Travel","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SHORT RANGE MODE:","color":"red","italic":false}','{"text":"→ SNEAK: Freeze entities (3 blocks)","color":"dark_red","italic":false}','{"text":"→ Remove NoAI, apply blindness","color":"dark_red","italic":false}'],custom_model_data=2,custom_data={tobi_offensive:1b,offensive_mode:0}] 1
execute if score @s tobi_offensive_mode matches 3 run tellraw @s {"text":"[Offensive] Switched to SHORT RANGE mode","color":"red","bold":true}
execute if score @s tobi_offensive_mode matches 3 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 0.8

# ============================================
# UPDATE tobi_offensive_mode SCORE
# ============================================
# Adds 1 then wraps at 4:  0→1→2→3→0
scoreboard players add @s tobi_offensive_mode 1
execute if score @s tobi_offensive_mode matches 4.. run scoreboard players set @s tobi_offensive_mode 0
