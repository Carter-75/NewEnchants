package com.carter.newenchants.system.state;

import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TargetCooldownMap {
    private final Map<UUID, Map<UUID, Map<String, Long>>> data = new HashMap<>();

    public boolean ready(LivingEntity owner, LivingEntity target, String key, long now, int cooldownTicks) {
        Map<UUID, Map<String, Long>> ownerMap = data.computeIfAbsent(owner.getUuid(), uuid -> new HashMap<>());
        Map<String, Long> targetMap = ownerMap.computeIfAbsent(target.getUuid(), uuid -> new HashMap<>());
        long threshold = targetMap.getOrDefault(key, 0L);
        if (now >= threshold) {
            targetMap.put(key, now + cooldownTicks);
            return true;
        }
        return false;
    }
}
