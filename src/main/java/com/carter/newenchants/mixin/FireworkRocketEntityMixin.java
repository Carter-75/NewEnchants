package com.carter.newenchants.mixin;

import com.carter.newenchants.system.RangedEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Entity {
    protected FireworkRocketEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)V", at = @At("TAIL"))
    private void newenchants$afterConstruct(World world, ItemStack stack, LivingEntity shooter, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        if (shooter instanceof PlayerEntity player) {
            RangedEventHooks.onFireworkLaunched(player);
        }
    }
}
