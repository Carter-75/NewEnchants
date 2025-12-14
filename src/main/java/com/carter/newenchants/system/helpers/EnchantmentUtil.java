package com.carter.newenchants.system.helpers;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;

public final class EnchantmentUtil {
    private EnchantmentUtil() {}

    public static int getLevel(LivingEntity entity, RegistryEntry<Enchantment> enchantment) {
        return EnchantmentHelper.getEquipmentLevel(enchantment, entity);
    }

    public static int getHeldLevel(LivingEntity entity, RegistryEntry<Enchantment> enchantment) {
        int main = EnchantmentHelper.getLevel(enchantment, entity.getMainHandStack());
        if (main > 0) {
            return main;
        }
        return EnchantmentHelper.getLevel(enchantment, entity.getOffHandStack());
    }
}
