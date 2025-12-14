package com.carter.newenchants.enchantments;

import com.carter.newenchants.system.data.EnchantmentSpec;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

public class CustomEnchantment extends Enchantment {
    private final int maxLevel;
    private final boolean treasureOnly;
    private final boolean randomSelectable;
    private final Predicate<ItemStack> predicate;
    private final EnchantmentSpec spec;

    public CustomEnchantment(Rarity weight,
                             EnchantmentTarget target,
                             EquipmentSlot[] slots,
                             int maxLevel,
                             boolean treasureOnly,
                             boolean randomSelectable,
                             Predicate<ItemStack> predicate,
                             EnchantmentSpec spec) {
        super(weight, target, slots);
        this.maxLevel = maxLevel;
        this.treasureOnly = treasureOnly;
        this.randomSelectable = randomSelectable;
        this.predicate = predicate;
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public boolean isTreasure() {
        return treasureOnly;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return randomSelectable;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return predicate.test(stack);
    }

    public EnchantmentSpec spec() {
        return spec;
    }
}
