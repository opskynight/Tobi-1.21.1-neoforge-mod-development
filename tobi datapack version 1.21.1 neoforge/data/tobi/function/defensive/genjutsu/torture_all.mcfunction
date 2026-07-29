# ============================================
# GENJUTSU TORTURE ALL (FIXED)
# ============================================
# Torture all mobs with genjutsu_target OR genjutsu_sneak_target tags

# Apply damage to raycast-targeted mobs (genjutsu_target tag)
execute as @e[tag=genjutsu_target] unless entity @s[tag=genjutsu_damaged] at @s run function tobi:defensive/genjutsu/apply_damage

# Apply damage to sneak-frozen mobs (genjutsu_sneak_target tag)
execute as @e[tag=genjutsu_sneak_target] unless entity @s[tag=genjutsu_damaged] at @s run function tobi:defensive/genjutsu/apply_damage

# Sound effect for player
playsound minecraft:entity.wither.hurt player @s ~ ~ ~ 0.8 0.5

# Particle burst around player - REDUCED from 50 to 20 particles
execute at @s run particle minecraft:dust{color:[0.5,0.0,0.0],scale:1.5} ~ ~1 ~ 0.8 0.8 0.8 0.2 20 force

# Message
tellraw @s {"text":"[Genjutsu] Torture activated!","color":"dark_red","bold":true}
