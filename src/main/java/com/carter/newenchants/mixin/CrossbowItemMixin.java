package com.carter.newenchants.mixin;

import com.carter.newenchants.system.RangedEventHooks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Inject(method = "shootAll", at = @At("TAIL"))
    private static void newenchants$afterShot(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, CallbackInfo ci) {
        RangedEventHooks.onCrossbowReleased(shooter, stack, world);
    }
}
