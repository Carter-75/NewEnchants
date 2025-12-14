package com.carter.newenchants.registry;

import com.carter.newenchants.NewEnchantsMod;
import com.carter.newenchants.system.data.EnchantmentSpec;
import com.carter.newenchants.system.data.EnchantmentSpecLoader;
import com.carter.newenchants.system.data.GearFamily;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModEnchantments {
    private static final Map<ModEnchantmentKey, Enchantment> ENCHANTMENTS = new EnumMap<>(ModEnchantmentKey.class);
    private static final Map<ModEnchantmentKey, EnchantmentSpec> SPECS = new EnumMap<>(ModEnchantmentKey.class);

    private ModEnchantments() {}

    public static void init() {
        if (!ENCHANTMENTS.isEmpty()) {
            return;
        }
        List<EnchantmentSpec> specs = EnchantmentSpecLoader.loadAll();
        for (EnchantmentSpec spec : specs) {
            ModEnchantmentKey key = ModEnchantmentKey.fromSlug(spec.slug());
            GearFamily family = GearFamily.fromLabel(spec.gearFamily());
            Identifier id = spec.identifier();
            Enchantment enchantment = Registry.register(Registries.ENCHANTMENT, id, family.createEnchantment(spec));
            ENCHANTMENTS.put(key, enchantment);
            SPECS.put(key, spec);
        }
        if (ENCHANTMENTS.size() != ModEnchantmentKey.values().length) {
            NewEnchantsMod.LOGGER.warn("Loaded {} enchantments but {} keys exist", ENCHANTMENTS.size(), ModEnchantmentKey.values().length);
        }
    }

    public static Enchantment get(ModEnchantmentKey key) {
        Enchantment enchantment = ENCHANTMENTS.get(key);
        if (enchantment == null) {
            throw new IllegalStateException("Enchantment " + key + " has not been registered");
        }
        return enchantment;
    }

    public static EnchantmentSpec spec(ModEnchantmentKey key) {
        EnchantmentSpec spec = SPECS.get(key);
        if (spec == null) {
            throw new IllegalStateException("Spec for " + key + " is missing");
        }
        return spec;
    }

    public static int getRegisteredCount() {
        return ENCHANTMENTS.size();
    }
}
