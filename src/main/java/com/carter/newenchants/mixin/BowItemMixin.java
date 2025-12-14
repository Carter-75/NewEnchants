package com.carter.newenchants.mixin;

import com.carter.newenchants.system.RangedEventHooks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void newenchants$onFired(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        RangedEventHooks.onBowReleased(user, stack, world, remainingUseTicks);
    }
}
