package com.carter.newenchants.registry;

import com.carter.newenchants.NewEnchantsMod;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ModEnchantmentKey {
    CHRONOBLADE_EDGE("chronoblade_edge"),
    BLOOD_OATH("blood_oath"),
    VOIDBREAKER("voidbreaker"),
    RADIANT_EXECUTION("radiant_execution"),
    PHANTOM_GUILLOTINE("phantom_guillotine"),
    RIFTED_LUNGE("rifted_lunge"),
    WITHER_SEVERANCE("wither_severance"),
    SOLAR_RIPOSTE("solar_riposte"),
    TEMPEST_VOLLEY("tempest_volley"),
    STARFALL_DRAW("starfall_draw"),
    UMBRAL_PIN("umbral_pin"),
    VERDANT_TIDE("verdant_tide"),
    AETHER_SALVO("aether_salvo"),
    SIEGE_RAM("siege_ram"),
    SLIPSTREAM_REPEATER("slipstream_repeater"),
    TITAN_HEW("titan_hew"),
    GROVE_SHATTER("grove_shatter"),
    WARCALLER("warcaller"),
    THUNDER_CHOP("thunder_chop"),
    LODE_HARMONIZER("lode_harmonizer"),
    SEISMIC_RESONANCE("seismic_resonance"),
    PRISM_SPLITTER("prism_splitter"),
    DEEP_ECHO("deep_echo"),
    TERRA_SURGE("terra_surge"),
    DUNE_VEIL("dune_veil"),
    BLOOM_BINDER("bloom_binder"),
    SOUL_HARVESTER("soul_harvester"),
    ABYSSAL_CHURN("abyssal_churn"),
    STORM_ANCHOR("storm_anchor"),
    LEVIATHAN_CALL("leviathan_call"),
    MINDWARD_VEIL("mindward_veil"),
    STARLIT_FOCUS("starlit_focus"),
    ABYSSAL_GAZE("abyssal_gaze"),
    DRAGONHEART_BULWARK("dragonheart_bulwark"),
    RADIANT_FURNACE("radiant_furnace"),
    UMBRA_RESERVOIR("umbra_reservoir"),
    RESONANT_STRIDE("resonant_stride"),
    BASTION_LATTICE("bastion_lattice"),
    TEMPEST_HARNESS("tempest_harness"),
    RIFTWALKER_STEPS("riftwalker_steps"),
    GLACIAL_CREST("glacial_crest"),
    EMBER_DASH("ember_dash"),
    BULWARK_CONDUCTOR("bulwark_conductor"),
    MIRROR_GUARD("mirror_guard"),
    SOLAR_THERMALS("solar_thermals"),
    TIDAL_LURE("tidal_lure"),
    ECHO_LINE("echo_line"),
    SILK_GARDENER("silk_gardener"),
    IMPACT_BLOOM("impact_bloom"),
    SHOCK_LANCER("shock_lancer");

    private static final Map<String, ModEnchantmentKey> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ModEnchantmentKey::slug, key -> key));

    private final String slug;

    ModEnchantmentKey(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public Identifier identifier() {
        return Identifier.of(NewEnchantsMod.MOD_ID, slug);
    }

    public static ModEnchantmentKey fromSlug(String slug) {
        ModEnchantmentKey key = LOOKUP.get(slug);
        if (key == null) {
            throw new IllegalArgumentException("Unknown enchantment slug: " + slug);
        }
        return key;
    }
}
