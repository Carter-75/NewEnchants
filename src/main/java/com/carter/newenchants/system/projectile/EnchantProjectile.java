package com.carter.newenchants.system.projectile;

/**
 * Simple bridge that lets mixins stash enchantment metadata on projectile entities.
 */
public interface EnchantProjectile {
    void newenchants$setTempestBonus(float bonus);
    float newenchants$getTempestBonus();

    void newenchants$setVerdantLevel(int level);
    int newenchants$getVerdantLevel();

    void newenchants$setUmbralLevel(int level);
    int newenchants$getUmbralLevel();

    void newenchants$setStarfallLevel(int level);
    int newenchants$getStarfallLevel();

    void newenchants$setAetherLevel(int level);
    int newenchants$getAetherLevel();

    void newenchants$setAetherFinal(boolean value);
    boolean newenchants$isAetherFinal();

    void newenchants$setSiegeLevel(int level);
    int newenchants$getSiegeLevel();

    void newenchants$setStormLevel(int level);
    int newenchants$getStormLevel();
}
