package com.carter.newenchants.registry;

import com.carter.newenchants.NewEnchantsMod;
import com.carter.newenchants.system.data.EnchantmentSpec;
import com.carter.newenchants.system.data.EnchantmentSpecLoader;
import com.carter.newenchants.system.data.GearFamily;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModEnchantments {
    private static final Map<ModEnchantmentKey, RegistryEntry.Reference<Enchantment>> ENCHANTMENTS = new EnumMap<>(ModEnchantmentKey.class);
    private static final Map<ModEnchantmentKey, EnchantmentSpec> SPECS = new EnumMap<>(ModEnchantmentKey.class);

    private ModEnchantments() {}

    public static void init() {
        if (!ENCHANTMENTS.isEmpty()) {
            return;
        }
        Registry<Enchantment> registry = locateEnchantmentRegistry();
        List<EnchantmentSpec> specs = EnchantmentSpecLoader.loadAll();
        for (EnchantmentSpec spec : specs) {
            ModEnchantmentKey key = ModEnchantmentKey.fromSlug(spec.slug());
            GearFamily family = GearFamily.fromLabel(spec.gearFamily());
            Identifier id = spec.identifier();
            Enchantment enchantment = family.createEnchantment(spec);
            RegistryKey<Enchantment> registryKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
            RegistryEntry.Reference<Enchantment> entry = Registry.registerReference(registry, registryKey, enchantment);
            ENCHANTMENTS.put(key, entry);
            SPECS.put(key, spec);
        }
        if (ENCHANTMENTS.size() != ModEnchantmentKey.values().length) {
            NewEnchantsMod.LOGGER.warn("Loaded {} enchantments but {} keys exist", ENCHANTMENTS.size(), ModEnchantmentKey.values().length);
        }
    }

    public static RegistryEntry.Reference<Enchantment> get(ModEnchantmentKey key) {
        RegistryEntry.Reference<Enchantment> enchantment = ENCHANTMENTS.get(key);
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

    @SuppressWarnings("unchecked")
    private static Registry<Enchantment> locateEnchantmentRegistry() {
        Registry<Enchantment> registry = (Registry<Enchantment>) Registries.REGISTRIES.get(RegistryKeys.ENCHANTMENT);
        return Objects.requireNonNull(registry, "Enchantment registry is not available");
    }
}
