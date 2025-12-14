package com.carter.newenchants.system.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class DamageUtil {
    private DamageUtil() {}

    public static void trueDamage(World world, LivingEntity attacker, LivingEntity target, float amount) {
        DamageSource source = world.getDamageSources().playerAttack(attacker instanceof net.minecraft.entity.player.PlayerEntity player ? player : null);
        target.damage(source, amount);
    }
}
