# ============================================
# GIVE KAMUI DIMENSIONAL STYLE COAS
# ============================================
# Usage: /function tobi:coas/give_dimensional

# Give to nearest player if no context, otherwise give to @s
execute unless entity @s run give @p carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Travel (Spectator)","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Tobi\'s dimensional abilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Travel → Dimension → Genjutsu","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"TRAVEL MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Spectator mode","color":"dark_aqua","italic":false}','{"text":"→ Still 5s: Survival mode","color":"dark_aqua","italic":false}'],custom_model_data=3,custom_data={tobi_dimensional:1b,dimensional_mode:0}] 1

execute if entity @s run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Travel (Spectator)","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Tobi\'s dimensional abilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Travel → Dimension → Genjutsu","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"TRAVEL MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Spectator mode","color":"dark_aqua","italic":false}','{"text":"→ Still 5s: Survival mode","color":"dark_aqua","italic":false}'],custom_model_data=3,custom_data={tobi_dimensional:1b,dimensional_mode:0}] 1

execute unless entity @s run tellraw @p {"text":"[Tobi] Received Kamui Dimensional Style! (Travel Mode)","color":"aqua","bold":true}
execute unless entity @s run tellraw @p {"text":"→ Swap to offhand (F key) to change modes","color":"dark_aqua"}

execute if entity @s run tellraw @s {"text":"[Tobi] Received Kamui Dimensional Style! (Travel Mode)","color":"aqua","bold":true}
execute if entity @s run tellraw @s {"text":"→ Swap to offhand (F key) to change modes","color":"dark_aqua"}