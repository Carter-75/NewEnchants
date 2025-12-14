package com.carter.newenchants.system.data;

import com.carter.newenchants.NewEnchantsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EnchantmentSpecLoader {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private EnchantmentSpecLoader() {}

    public static List<EnchantmentSpec> loadAll() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer(NewEnchantsMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Unable to locate NewEnchants mod container"));
        Path specsDir = container.findPath("enchants")
                .orElseThrow(() -> new IllegalStateException("Missing enchants/ resource directory"));

        List<EnchantmentSpec> specs = new ArrayList<>();
        try (var stream = Files.walk(specsDir, 1)) {
            stream.filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> specs.add(readSpec(path)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan enchants directory", e);
        }
        if (specs.isEmpty()) {
            throw new IllegalStateException("No enchantment specs were loaded from enchants/");
        }
        return specs;
    }

    private static EnchantmentSpec readSpec(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            EnchantmentSpec spec = GSON.fromJson(reader, EnchantmentSpec.class);
            if (spec == null) {
                throw new IllegalStateException("Spec " + path + " is empty");
            }
            return spec;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read spec " + path, e);
        }
    }
}
