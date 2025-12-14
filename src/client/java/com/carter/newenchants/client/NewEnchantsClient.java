package com.carter.newenchants.client;

import com.carter.newenchants.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.GlowParticle;

public final class NewEnchantsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.SOLAR_SPARK, GlowParticle.GlowFactory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.VOID_RIFT, GlowParticle.GlowFactory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.VERDANT_RING, GlowParticle.GlowFactory::new);
    }
}
