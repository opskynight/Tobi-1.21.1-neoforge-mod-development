# Main controller

# Initialize scores if they don't exist
execute as @a unless score @s spiral_state = @s spiral_state run scoreboard players set @s spiral_state 0
execute as @a unless score @s spiral_scale = @s spiral_scale run scoreboard players set @s spiral_scale 0
execute as @a unless score @s spiral_timer = @s spiral_timer run scoreboard players set @s spiral_timer 0
execute as @a unless score @s spiral_rotation = @s spiral_rotation run scoreboard players set @s spiral_rotation 0

# DEBUG - Show what's happening
execute as @a[scores={tobi_slot=6},predicate=tobi:is_sneaking,gamemode=survival] run title @s actionbar ["",{"text":"Slot: ","color":"white"},{"score":{"name":"@s","objective":"tobi_slot"},"color":"green"},{"text":" | State: ","color":"white"},{"score":{"name":"@s","objective":"spiral_state"},"color":"aqua"},{"text":" | Rotation: ","color":"white"},{"score":{"name":"@s","objective":"spiral_rotation"},"color":"gold"}]

# Show state even when NOT sneaking (for debug)
execute as @a[scores={spiral_state=1..}] unless predicate tobi:is_sneaking run title @s actionbar [{"text":"NOT SNEAKING | State: ","color":"red"},{"score":{"name":"@s","objective":"spiral_state"},"color":"yellow"},{"text":" | Rotation: ","color":"white"},{"score":{"name":"@s","objective":"spiral_rotation"},"color":"gold"}]

# Detect slot 6 + sneak to START animation (only if not already active)
execute as @a[scores={tobi_slot=6},predicate=tobi:is_sneaking,gamemode=survival] unless score @s spiral_state matches 1.. run function tobi:test_spiral/trigger_deploy

# STOP animation if player releases sneak or changes slot
execute as @a[scores={spiral_state=1}] unless score @s tobi_slot matches 6 run function tobi:test_spiral/trigger_withdraw
execute as @a[scores={spiral_state=1}] unless predicate tobi:is_sneaking run function tobi:test_spiral/trigger_withdraw
execute as @a[scores={spiral_state=3}] unless score @s tobi_slot matches 6 run function tobi:test_spiral/trigger_withdraw
execute as @a[scores={spiral_state=3}] unless predicate tobi:is_sneaking run function tobi:test_spiral/trigger_withdraw

# Run animation states
execute as @a[scores={spiral_state=1}] run function tobi:test_spiral/states/deploying
execute as @a[scores={spiral_state=2}] run function tobi:test_spiral/states/withdrawing
execute as @a[scores={spiral_state=3}] run function tobi:test_spiral/states/idle_spin

# Render the rotating spiral using pre-generated frames
execute as @a[scores={spiral_state=1..3}] run function tobi:test_spiral/render_spiral