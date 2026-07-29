# ============================================
# UPDATE COAS TO RETURN MODE (MACRO)
# ============================================
# Shows stored location in COAS lore
# Called with: function tobi:waypoint/update_coas_return with storage tobi:temp

# Clear old COAS
clear @s carrot_on_a_stick[custom_data~{tobi_waypoint:1b}]

# Give Return variant with stored location in lore
$give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Return","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Return to waypoint","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SAVED LOCATION:","color":"gold","italic":false}','{"text":"→ $(dimension)","color":"yellow","italic":false}','{"text":"→ X: $(x)","color":"yellow","italic":false}','{"text":"→ Y: $(y)","color":"yellow","italic":false}','{"text":"→ Z: $(z)","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SNEAK 5s: Return to waypoint","color":"aqua","italic":false}'],custom_model_data=4,custom_data={tobi_waypoint:1b}] 1
