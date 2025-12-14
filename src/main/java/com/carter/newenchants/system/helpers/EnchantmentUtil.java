package com.carter.newenchants.system.helpers;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public final class EnchantmentUtil {
    private EnchantmentUtil() {}

    public static int getLevel(LivingEntity entity, Enchantment enchantment) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                total = Math.max(total, EnchantmentHelper.getLevel(enchantment, stack));
            }
        }
        return total;
    }

    public static int getHeldLevel(LivingEntity entity, Enchantment enchantment) {
        ItemStack main = entity.getMainHandStack();
        int level = EnchantmentHelper.getLevel(enchantment, main);
        if (level > 0) {
            return level;
        }
        return EnchantmentHelper.getLevel(enchantment, entity.getOffHandStack());
    }
}
