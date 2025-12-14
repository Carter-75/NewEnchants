package com.carter.newenchants;

import net.fabricmc.api.ModInitializer;

public final class NewEnchantsEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        NewEnchantsMod.init();
    }
}
