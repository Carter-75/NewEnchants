package com.carter.newenchants.system.util;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class ParticleUtil {
    private ParticleUtil() {}

    public static void burst(ServerWorld world, ParticleEffect effect, Vec3d center, int count, double spread, double speed) {
        world.spawnParticles(effect, center.x, center.y, center.z, count, spread, spread, spread, speed);
    }
}
