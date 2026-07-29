# Reset score to 0 for entities that were successfully returned
execute as @e[scores={tobi_entity_marked=40..}] at @s if entity @a[scores={tobi_slot=4,tobi_charge=40..},distance=..5] run scoreboard players set @s tobi_entity_marked 0

# Clear blindness immediately upon return
execute as @e[scores={tobi_entity_marked=0}] run effect clear @s minecraft:blindness

# Remove persistence requirement when returned
execute as @e[scores={tobi_entity_marked=0}] run data merge entity @s {PersistenceRequired:0b}