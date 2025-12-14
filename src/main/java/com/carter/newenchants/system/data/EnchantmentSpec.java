package com.carter.newenchants.system.data;

import com.carter.newenchants.NewEnchantsMod;
import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

public record EnchantmentSpec(
        String id,
        String slug,
        String name,
        int index,
        @SerializedName("gear_family") String gearFamily,
        @SerializedName("applies_to") String appliesTo,
        @SerializedName("max_level") int maxLevel,
        List<LevelEffect> effects,
        Recipe recipe
) {
    public Identifier identifier() {
        if (id != null && !id.isBlank()) {
            return Identifier.of(id);
        }
        return Identifier.of(NewEnchantsMod.MOD_ID, slug.toLowerCase(Locale.ROOT));
    }

    public record LevelEffect(int level, String summary) {}

    public record Recipe(String type, List<List<String>> rows, Result result) {
        public record Result(String item, @SerializedName("stored_enchantment") String storedEnchantment) {}
    }
}
