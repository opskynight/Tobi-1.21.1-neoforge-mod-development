package com.tobi.tobimod.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityFlagAccessor {
    @Invoker("getSharedFlag")
    boolean tobimod$invokeGetSharedFlag(int flag);
}
