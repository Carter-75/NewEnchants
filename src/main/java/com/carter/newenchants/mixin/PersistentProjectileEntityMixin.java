package com.carter.newenchants.mixin;

import com.carter.newenchants.system.RangedEventHooks;
import com.carter.newenchants.system.projectile.EnchantProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin implements EnchantProjectile {
    private float newenchants$tempestBonus;
    private int newenchants$verdantLevel;
    private int newenchants$umbralLevel;
    private int newenchants$starfallLevel;
    private int newenchants$aetherLevel;
    private boolean newenchants$aetherFinal;
    private int newenchants$siegeLevel;
    private int newenchants$stormLevel;

    @Override
    public void newenchants$setTempestBonus(float bonus) {
        this.newenchants$tempestBonus = bonus;
    }

    @Override
    public float newenchants$getTempestBonus() {
        return this.newenchants$tempestBonus;
    }

    @Override
    public void newenchants$setVerdantLevel(int level) {
        this.newenchants$verdantLevel = level;
    }

    @Override
    public int newenchants$getVerdantLevel() {
        return this.newenchants$verdantLevel;
    }

    @Override
    public void newenchants$setUmbralLevel(int level) {
        this.newenchants$umbralLevel = level;
    }

    @Override
    public int newenchants$getUmbralLevel() {
        return this.newenchants$umbralLevel;
    }

    @Override
    public void newenchants$setStarfallLevel(int level) {
        this.newenchants$starfallLevel = level;
    }

    @Override
    public int newenchants$getStarfallLevel() {
        return this.newenchants$starfallLevel;
    }

    @Override
    public void newenchants$setAetherLevel(int level) {
        this.newenchants$aetherLevel = level;
    }

    @Override
    public int newenchants$getAetherLevel() {
        return this.newenchants$aetherLevel;
    }

    @Override
    public void newenchants$setAetherFinal(boolean value) {
        this.newenchants$aetherFinal = value;
    }

    @Override
    public boolean newenchants$isAetherFinal() {
        return this.newenchants$aetherFinal;
    }

    @Override
    public void newenchants$setSiegeLevel(int level) {
        this.newenchants$siegeLevel = level;
    }

    @Override
    public int newenchants$getSiegeLevel() {
        return this.newenchants$siegeLevel;
    }

    @Override
    public void newenchants$setStormLevel(int level) {
        this.newenchants$stormLevel = level;
    }

    @Override
    public int newenchants$getStormLevel() {
        return this.newenchants$stormLevel;
    }

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void newenchants$handleOwnerAssignment(Entity owner, CallbackInfo ci) {
        RangedEventHooks.onProjectileOwnerAssigned((PersistentProjectileEntity) (Object) this);
    }

    @Inject(method = "onBlockHit", at = @At("TAIL"))
    private void newenchants$afterBlockHit(BlockHitResult hitResult, CallbackInfo ci) {
        RangedEventHooks.onProjectileBlockHit((PersistentProjectileEntity) (Object) this, hitResult);
    }

    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void newenchants$afterEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        RangedEventHooks.onProjectileEntityHit((PersistentProjectileEntity) (Object) this, hitResult);
    }
}
