package com.carter.newenchants.system.data;

import com.carter.newenchants.enchantments.CustomEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GearFamily {
    SWORDS("SWORDS", EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof SwordItem),
    SWORDS_AND_AXES("SWORDS & AXES", EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem),
    AXES("AXES", EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof AxeItem),
    BOWS("BOWS", EnchantmentTarget.BOW, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof BowItem),
    CROSSBOWS("CROSSBOWS", EnchantmentTarget.BOW, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof CrossbowItem),
    TRIDENTS("TRIDENTS", EnchantmentTarget.BOW, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof TridentItem),
    PICKAXES("PICKAXES", EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof PickaxeItem),
    SHOVELS("SHOVELS", EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof ShovelItem),
    HOES("HOES", EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof HoeItem),
    MACES("MACES", EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof MaceItem),
    HELMETS("HELMETS", EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD}, stack -> armorMatches(stack, ArmorItem.Type.HELMET)),
    CHESTPLATES("CHESTPLATES", EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.CHEST}, stack -> armorMatches(stack, ArmorItem.Type.CHESTPLATE)),
    LEGGINGS("LEGGINGS", EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.LEGS}, stack -> armorMatches(stack, ArmorItem.Type.LEGGINGS)),
    BOOTS("BOOTS", EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.FEET}, stack -> armorMatches(stack, ArmorItem.Type.BOOTS)),
    SHIELDS("SHIELDS", EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}, stack -> stack.getItem() instanceof ShieldItem),
    ELYTRA("ELYTRA", EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.CHEST}, stack -> stack.getItem() instanceof ElytraItem || stack.isOf(Items.ELYTRA)),
    FISHING_RODS("FISHING RODS", EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof FishingRodItem),
    SHEARS("SHEARS", EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND}, stack -> stack.getItem() instanceof ShearsItem);

    private static final Map<String, GearFamily> LOOKUP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(family -> normalizeKey(family.label), family -> family));

    private final String label;
    private final EnchantmentTarget target;
    private final EquipmentSlot[] slots;
    private final Predicate<ItemStack> predicate;

    GearFamily(String label, EnchantmentTarget target, EquipmentSlot[] slots, Predicate<ItemStack> predicate) {
        this.label = label;
        this.target = target;
        this.slots = slots;
        this.predicate = predicate;
    }

    public CustomEnchantment createEnchantment(EnchantmentSpec spec) {
        return new CustomEnchantment(Enchantment.Rarity.RARE, target, slots, spec.maxLevel(), true, false, predicate, spec);
    }

    public static GearFamily fromLabel(String label) {
        GearFamily family = LOOKUP.get(normalizeKey(label));
        if (family == null) {
            throw new IllegalArgumentException("Unsupported gear family label: " + label);
        }
        return family;
    }

    private static boolean armorMatches(ItemStack stack, ArmorItem.Type type) {
        return stack.getItem() instanceof ArmorItem armor && armor.getType() == type;
    }

    private static String normalizeKey(String label) {
        return label == null ? "" : label.toUpperCase(Locale.ROOT).replace("&", "AND").replaceAll("[^A-Z0-9]+", "_");
    }
}
