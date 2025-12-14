package com.carter.newenchants.mixin;

import com.carter.newenchants.system.RangedEventHooks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void newenchants$onTridentThrow(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        RangedEventHooks.onTridentReleased(user, stack, world);
    }
}
