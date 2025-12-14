package com.carter.newenchants.system.data;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GearFamily {
    SWORDS("SWORDS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof SwordItem),
    SWORDS_AND_AXES("SWORDS & AXES", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof SwordItem || item instanceof AxeItem),
    AXES("AXES", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof AxeItem),
    BOWS("BOWS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof BowItem),
    CROSSBOWS("CROSSBOWS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof CrossbowItem),
    TRIDENTS("TRIDENTS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof TridentItem),
    PICKAXES("PICKAXES", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof PickaxeItem),
    SHOVELS("SHOVELS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof ShovelItem),
    HOES("HOES", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof HoeItem),
    MACES("MACES", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof MaceItem),
    HELMETS("HELMETS", new EquipmentSlot[]{EquipmentSlot.HEAD}, item -> armorMatches(item, ArmorItem.Type.HELMET)),
    CHESTPLATES("CHESTPLATES", new EquipmentSlot[]{EquipmentSlot.CHEST}, item -> armorMatches(item, ArmorItem.Type.CHESTPLATE)),
    LEGGINGS("LEGGINGS", new EquipmentSlot[]{EquipmentSlot.LEGS}, item -> armorMatches(item, ArmorItem.Type.LEGGINGS)),
    BOOTS("BOOTS", new EquipmentSlot[]{EquipmentSlot.FEET}, item -> armorMatches(item, ArmorItem.Type.BOOTS)),
    SHIELDS("SHIELDS", new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}, item -> item instanceof ShieldItem),
    ELYTRA("ELYTRA", new EquipmentSlot[]{EquipmentSlot.CHEST}, item -> item instanceof ElytraItem || item == Items.ELYTRA),
    FISHING_RODS("FISHING RODS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof FishingRodItem),
    SHEARS("SHEARS", new EquipmentSlot[]{EquipmentSlot.MAINHAND}, item -> item instanceof ShearsItem);

    private static final Map<String, GearFamily> LOOKUP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(family -> normalizeKey(family.label), family -> family));

    private final String label;
    private final AttributeModifierSlot[] attributeSlots;
    private final Predicate<Item> predicate;
    private RegistryEntryList<Item> cachedSupportedItems;

    GearFamily(String label, EquipmentSlot[] slots, Predicate<Item> predicate) {
        this.label = label;
        this.attributeSlots = Arrays.stream(slots)
                .map(AttributeModifierSlot::forEquipmentSlot)
                .distinct()
                .toArray(AttributeModifierSlot[]::new);
        this.predicate = predicate;
    }

    public Enchantment createEnchantment(EnchantmentSpec spec) {
        RegistryEntryList<Item> supported = resolveSupportedItems();
        int maxLevel = Math.max(1, spec.maxLevel());
        int weight = 3;
        Enchantment.Cost minCost = Enchantment.leveledCost(10, 8);
        Enchantment.Cost maxCost = Enchantment.leveledCost(25, 10);
        int anvilCost = Math.max(1, maxLevel);
        Enchantment.Definition definition = Enchantment.definition(supported, weight, maxLevel, minCost, maxCost, anvilCost, attributeSlots);
        return Enchantment.builder(definition).build(spec.identifier());
    }

    public static GearFamily fromLabel(String label) {
        GearFamily family = LOOKUP.get(normalizeKey(label));
        if (family == null) {
            throw new IllegalArgumentException("Unsupported gear family label: " + label);
        }
        return family;
    }

    private RegistryEntryList<Item> resolveSupportedItems() {
        if (cachedSupportedItems == null) {
            List<RegistryEntry<Item>> matches = new ArrayList<>(Registries.ITEM.streamEntries()
                    .filter(entry -> predicate.test(entry.value()))
                    .toList());
            cachedSupportedItems = matches.isEmpty() ? RegistryEntryList.empty() : RegistryEntryList.of(matches);
        }
        return cachedSupportedItems;
    }

    private static boolean armorMatches(Item item, ArmorItem.Type type) {
        return item instanceof ArmorItem armor && armor.getType() == type;
    }

    private static String normalizeKey(String label) {
        return label == null ? "" : label.toUpperCase(Locale.ROOT).replace("&", "AND").replaceAll("[^A-Z0-9]+", "_");
    }
}
