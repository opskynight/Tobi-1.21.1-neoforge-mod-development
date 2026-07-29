# ============================================
# KAMUI TRAVEL - DEACTIVATE
# ============================================
# Return to survival mode after stillness detected

# Switch back to survival
gamemode survival @s

# Reset scores
scoreboard players set @s tobi_kamui_active 0
scoreboard players set @s tobi_kamui_stillness 0
scoreboard players set @s tobi_kamui_charge 0

# ============================================
# SAFETY: Re-give the Travel COAS if it's missing.
# Spectator mode can cause item weirdness — this makes sure
# the stick is back in hand so tobi_offensive stays 1.
# ============================================
execute unless items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b}] run give @s carrot_on_a_stick[unbreakable={},custom_name='{"text":"Kamui Offensive (Travel)","color":"aqua","bold":true,"italic":false}',lore=['{"text":"Tobi\'s offensive capabilities","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"SWAP TO OFFHAND: Change Mode","color":"gold","italic":false}','{"text":"→ Short → Long → Return → Travel","color":"yellow","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"TRAVEL MODE:","color":"aqua","italic":false}','{"text":"→ SNEAK 5s: Spectator mode","color":"dark_aqua","italic":false}','{"text":"→ Stand still 3s: Return to survival","color":"dark_aqua","italic":false}'],custom_model_data=2,custom_data={tobi_offensive:1b,offensive_mode:3}] 1

# Messages and effects
tellraw @s {"text":"[Kamui Travel] Returned to survival mode.","color":"green","bold":true}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 2
execute at @s run particle minecraft:portal ~ ~1 ~ 0.5 0.5 0.5 1 50 force
