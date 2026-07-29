# ============================================
# GIVE KAMUI DEFENSIVE STYLE COAS
# ============================================
# Usage: /function tobi:coas/give_defensive
# Or target specific player: /execute as PlayerName run function tobi:coas/give_defensive

# Give to nearest player if no context, otherwise give to @s
execute unless entity @s run give @p carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Defensive Style","color":"dark_red","bold":true,"italic":false}',lore=['{"text":"Tobi\'s signature intangibility","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"PASSIVE: Invulnerability","color":"gold","italic":false}','{"text":"→ No damage, no projectiles","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SNEAK: Underground Phasing","color":"aqua","italic":false}','{"text":"→ Sink through ground","color":"dark_aqua","italic":false}','{"text":"→ Spectator mode while underground","color":"dark_aqua","italic":false}','{"text":"→ Release sneak to surface","color":"dark_aqua","italic":false}'],custom_model_data=1,custom_data={tobi_defensive:1b}] 1

execute if entity @s run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Defensive Style","color":"dark_red","bold":true,"italic":false}',lore=['{"text":"Tobi\'s signature intangibility","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"PASSIVE: Invulnerability","color":"gold","italic":false}','{"text":"→ No damage, no projectiles","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SNEAK: Underground Phasing","color":"aqua","italic":false}','{"text":"→ Sink through ground","color":"dark_aqua","italic":false}','{"text":"→ Spectator mode while underground","color":"dark_aqua","italic":false}','{"text":"→ Release sneak to surface","color":"dark_aqua","italic":false}'],custom_model_data=1,custom_data={tobi_defensive:1b}] 1

execute unless entity @s run tellraw @p {"text":"[Tobi] Received Kamui Defensive Style!","color":"dark_red","bold":true}
execute if entity @s run tellraw @s {"text":"[Tobi] Received Kamui Defensive Style!","color":"dark_red","bold":true}
