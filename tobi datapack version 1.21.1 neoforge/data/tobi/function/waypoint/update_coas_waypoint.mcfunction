# ============================================
# UPDATE COAS TO WAYPOINT MODE
# ============================================
# Resets COAS back to normal after returning

# Clear old COAS
clear @s carrot_on_a_stick[custom_data~{tobi_waypoint:1b}]

# Give normal Waypoint variant
give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Waypoint","color":"dark_purple","bold":true,"italic":false}',lore=['{"text":"Personal pocket dimension","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"WAYPOINT MODE:","color":"dark_purple","italic":false}','{"text":"→ SNEAK 5s: Warp to void","color":"light_purple","italic":false}','{"text":"→ Saves your exact location","color":"light_purple","italic":false}','{"text":"→ In void: Changes to Return mode","color":"light_purple","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"RETURN MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Return to waypoint","color":"dark_aqua","italic":false}','{"text":"→ Teleports to saved spot","color":"dark_aqua","italic":false}'],custom_model_data=4,custom_data={tobi_waypoint:1b}] 1
