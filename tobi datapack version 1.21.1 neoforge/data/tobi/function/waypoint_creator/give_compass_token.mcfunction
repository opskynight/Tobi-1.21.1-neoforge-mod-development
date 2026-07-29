# ============================================
# GIVE COMPASS TOKEN (MACRO)
# ============================================
# Gives compass with stored coordinates in NBT

$give @s minecraft:compass[\
custom_name='{"text":"Waypoint Token","color":"light_purple","bold":true}',\
lore=[\
'{"text":"Coordinates stored","color":"gray","italic":false}',\
'{"text":"$(token_dim)","color":"aqua"}',\
'{"text":"X: $(token_x), Y: $(token_y), Z: $(token_z)","color":"yellow"}',\
'{"text":""}',\
'{"text":"SNEAK + RIGHT CLICK to teleport","color":"green","bold":true}'\
],\
custom_data={\
waypoint_token:1b,\
token_x:$(token_x)d,\
token_y:$(token_y)d,\
token_z:$(token_z)d,\
token_dim:"$(token_dim)"\
}\
] 1