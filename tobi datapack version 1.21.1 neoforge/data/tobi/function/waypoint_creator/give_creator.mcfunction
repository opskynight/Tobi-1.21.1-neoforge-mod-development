# ============================================
# GIVE WAYPOINT CREATOR
# ============================================
# Usage: /function tobi:waypoint_creator/give_creator
# Creates compass tokens with stored coordinates

# Give to nearest player if no context, otherwise give to @s
execute unless entity @s run give @p carrot_on_a_stick[unbreakable={},custom_name='{"text":"Waypoint Creator","color":"gold","bold":true,"italic":false}',lore=['{"text":"Creates location markers","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RIGHT CLICK: Create Waypoint Token","color":"aqua","italic":false}','{"text":"→ Spawns a compass with your location","color":"dark_aqua","italic":false}','{"text":"→ Use the compass to teleport back","color":"dark_aqua","italic":false}'],custom_model_data=10,custom_data={waypoint_creator:1b}] 1

execute if entity @s run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Waypoint Creator","color":"gold","bold":true,"italic":false}',lore=['{"text":"Creates location markers","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RIGHT CLICK: Create Waypoint Token","color":"aqua","italic":false}','{"text":"→ Spawns a compass with your location","color":"dark_aqua","italic":false}','{"text":"→ Use the compass to teleport back","color":"dark_aqua","italic":false}'],custom_model_data=10,custom_data={waypoint_creator:1b}] 1

execute unless entity @s run tellraw @p {"text":"[Waypoint Creator] Received!","color":"gold","bold":true}
execute unless entity @s run tellraw @p {"text":"→ Right-click to create waypoint tokens","color":"yellow"}

execute if entity @s run tellraw @s {"text":"[Waypoint Creator] Received!","color":"gold","bold":true}
execute if entity @s run tellraw @s {"text":"→ Right-click to create waypoint tokens","color":"yellow"}
