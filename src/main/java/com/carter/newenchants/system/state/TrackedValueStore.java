package com.carter.newenchants.system.state;

import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TrackedValueStore {
    private final Map<UUID, Map<String, Double>> values = new HashMap<>();

    public double add(LivingEntity entity, String key, double delta, double min, double max) {
        Map<String, Double> entityMap = values.computeIfAbsent(entity.getUuid(), uuid -> new HashMap<>());
        double next = Math.max(min, Math.min(max, entityMap.getOrDefault(key, 0.0) + delta));
        entityMap.put(key, next);
        return next;
    }

    public double get(LivingEntity entity, String key) {
        Map<String, Double> entityMap = values.get(entity.getUuid());
        if (entityMap == null) {
            return 0;
        }
        return entityMap.getOrDefault(key, 0.0);
    }

    public void set(LivingEntity entity, String key, double value) {
        values.computeIfAbsent(entity.getUuid(), uuid -> new HashMap<>()).put(key, value);
    }

    public void reset(LivingEntity entity, String key) {
        Map<String, Double> entityMap = values.get(entity.getUuid());
        if (entityMap != null) {
            entityMap.remove(key);
        }
    }
}
