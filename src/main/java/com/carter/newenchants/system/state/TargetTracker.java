package com.carter.newenchants.system.state;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TargetTracker {
    private final Map<UUID, UUID> lastTarget = new HashMap<>();

    public void set(LivingEntity attacker, Entity target) {
        if (target == null) {
            lastTarget.remove(attacker.getUuid());
        } else {
            lastTarget.put(attacker.getUuid(), target.getUuid());
        }
    }

    public boolean isSameTarget(LivingEntity attacker, Entity target) {
        if (target == null) {
            return false;
        }
        return target.getUuid().equals(lastTarget.get(attacker.getUuid()));
    }
}
