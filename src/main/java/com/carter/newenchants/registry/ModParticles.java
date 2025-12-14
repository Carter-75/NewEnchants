package com.carter.newenchants.registry;

import com.carter.newenchants.NewEnchantsMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
    public static final SimpleParticleType SOLAR_SPARK = register("solar_spark");
    public static final SimpleParticleType VOID_RIFT = register("void_rift");
    public static final SimpleParticleType VERDANT_RING = register("verdant_ring");

    private ModParticles() {}

    private static SimpleParticleType register(String name) {
        Identifier id = Identifier.of(NewEnchantsMod.MOD_ID, name);
        return Registry.register(Registries.PARTICLE_TYPE, id, FabricParticleTypes.simple(true));
    }

    public static void init() {
        // static init
    }
}
