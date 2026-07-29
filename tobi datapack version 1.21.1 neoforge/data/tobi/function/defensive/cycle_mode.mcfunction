# ============================================
# DEFENSIVE MODE SWITCHING
# ============================================
# Cycles between Invulnerability (0) → Genjutsu (1) → Invulnerability (0)

# Mark that we've processed this swap
scoreboard players set @s tobi_offhand_swap 1

# ============================================
# INVULNERABILITY (0) → GENJUTSU (1)
# ============================================
execute if score @s tobi_defensive_mode matches 0 run clear @s carrot_on_a_stick[custom_data~{tobi_defensive:1b}]
execute if score @s tobi_defensive_mode matches 0 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Defensive (Genjutsu)","color":"red","bold":true,"italic":false}',lore=['{"text":"Tobi\'s signature abilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Invulnerability → Genjutsu","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"GENJUTSU MODE:","color":"red","italic":false}','{"text":"→ AIM: Freeze mobs (20 blocks, 5x5 area)","color":"dark_red","italic":false}','{"text":"→ SNEAK: Freeze 10x10 area (5s)","color":"dark_red","italic":false}','{"text":"→ RIGHT CLICK: Torture frozen mobs","color":"dark_red","italic":false}','{"text":"   (1/3 max HP, execute <20%)","color":"dark_red","italic":false}'],custom_model_data=1,custom_data={tobi_defensive:1b,defensive_mode:1}] 1
execute if score @s tobi_defensive_mode matches 0 run tellraw @s {"text":"[Defensive] Switched to GENJUTSU mode","color":"red","bold":true}
execute if score @s tobi_defensive_mode matches 0 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 0.8

# ============================================
# GENJUTSU (1) → INVULNERABILITY (0)
# ============================================
execute if score @s tobi_defensive_mode matches 1 run clear @s carrot_on_a_stick[custom_data~{tobi_defensive:1b}]
execute if score @s tobi_defensive_mode matches 1 run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Defensive (Invulnerability)","color":"dark_red","bold":true,"italic":false}',lore=['{"text":"Tobi\'s signature abilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Invulnerability → Genjutsu","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"INVULNERABILITY MODE:","color":"dark_red","italic":false}','{"text":"→ PASSIVE: No damage, no projectiles","color":"red","italic":false}','{"text":"→ SNEAK: Underground phasing","color":"aqua","italic":false}'],custom_model_data=1,custom_data={tobi_defensive:1b,defensive_mode:0}] 1
execute if score @s tobi_defensive_mode matches 1 run tellraw @s {"text":"[Defensive] Switched to INVULNERABILITY mode","color":"dark_red","bold":true}
execute if score @s tobi_defensive_mode matches 1 run playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1.5
