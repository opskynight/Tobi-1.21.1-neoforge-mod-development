# ============================================
# GIVE KAMUI WAYPOINT COAS
# ============================================
# Usage: /function tobi:coas/give_waypoint

# Give to nearest player if no context, otherwise give to @s
execute unless entity @s run give @p carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Waypoint","color":"dark_purple","bold":true,"italic":false}',lore=['{"text":"Personal pocket dimension","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"WAYPOINT MODE:","color":"dark_purple","italic":false}','{"text":"→ SNEAK 5s: Warp to void","color":"light_purple","italic":false}','{"text":"→ Saves your exact location","color":"light_purple","italic":false}','{"text":"→ In void: Changes to Return mode","color":"light_purple","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RETURN MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Return to waypoint","color":"dark_aqua","italic":false}','{"text":"→ Teleports to saved spot","color":"dark_aqua","italic":false}'],custom_model_data=4,custom_data={tobi_waypoint:1b}] 1

execute if entity @s run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Waypoint","color":"dark_purple","bold":true,"italic":false}',lore=['{"text":"Personal pocket dimension","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"WAYPOINT MODE:","color":"dark_purple","italic":false}','{"text":"→ SNEAK 5s: Warp to void","color":"light_purple","italic":false}','{"text":"→ Saves your exact location","color":"light_purple","italic":false}','{"text":"→ In void: Changes to Return mode","color":"light_purple","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RETURN MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Return to waypoint","color":"dark_aqua","italic":false}','{"text":"→ Teleports to saved spot","color":"dark_aqua","italic":false}'],custom_model_data=4,custom_data={tobi_waypoint:1b}] 1

execute unless entity @s run tellraw @p {"text":"[Tobi] Received Kamui Waypoint!","color":"dark_purple","bold":true}
execute if entity @s run tellraw @s {"text":"[Tobi] Received Kamui Waypoint!","color":"dark_purple","bold":true}
