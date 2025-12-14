package com.carter.newenchants.system.state;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownMap {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean ready(LivingEntity entity, String key, long currentTick, int cooldownTicks) {
        Map<String, Long> entityMap = cooldowns.computeIfAbsent(entity.getUuid(), uuid -> new HashMap<>());
        long threshold = entityMap.getOrDefault(key, 0L);
        if (currentTick >= threshold) {
            entityMap.put(key, currentTick + cooldownTicks);
            return true;
        }
        return false;
    }

    public long ticksRemaining(LivingEntity entity, String key, long currentTick) {
        Map<String, Long> entityMap = cooldowns.get(entity.getUuid());
        if (entityMap == null) {
            return 0;
        }
        long threshold = entityMap.getOrDefault(key, 0L);
        return Math.max(0, threshold - currentTick);
    }

    public void reset(LivingEntity entity, String key) {
        Map<String, Long> entityMap = cooldowns.get(entity.getUuid());
        if (entityMap != null) {
            entityMap.remove(key);
        }
    }
}
