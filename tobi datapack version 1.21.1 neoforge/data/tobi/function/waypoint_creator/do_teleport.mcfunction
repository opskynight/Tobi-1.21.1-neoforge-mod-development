# ============================================
# DO TELEPORT (MACRO)
# ============================================
# Teleports player to stored coordinates

# Debug
$tellraw @s [{"text":"[DEBUG] Teleporting to: $(token_dim) $(token_x) $(token_y) $(token_z)","color":"yellow"}]

# Teleport
$execute in $(token_dim) run tp @s $(token_x) $(token_y) $(token_z)

# Effects
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 2
execute at @s run particle minecraft:portal ~ ~1 ~ 0.5 0.5 0.5 1 50 force

# Success
tellraw @s {"text":"[Waypoint] Teleported!","color":"light_purple","bold":true}