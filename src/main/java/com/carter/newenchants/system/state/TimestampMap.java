package com.carter.newenchants.system.state;

import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TimestampMap {
    private final Map<UUID, Map<String, Long>> stamps = new HashMap<>();

    public void mark(LivingEntity entity, String key, long value) {
        stamps.computeIfAbsent(entity.getUuid(), uuid -> new HashMap<>()).put(key, value);
    }

    public long get(LivingEntity entity, String key) {
        Map<String, Long> entityMap = stamps.get(entity.getUuid());
        if (entityMap == null) {
            return 0L;
        }
        return entityMap.getOrDefault(key, 0L);
    }
}
