package com.carter.newenchants;

import com.carter.newenchants.registry.ModEnchantments;
import com.carter.newenchants.registry.ModParticles;
import com.carter.newenchants.system.EnchantmentEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NewEnchantsMod {
    public static final String MOD_ID = "newenchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private NewEnchantsMod() {}

    public static void init() {
        ModParticles.init();
        ModEnchantments.init();
        EnchantmentEvents.init();
        LOGGER.info("New Enchants loaded with {} enchantments", ModEnchantments.getRegisteredCount());
    }
}
