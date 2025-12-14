package com.carter.newenchants.system;

import com.carter.newenchants.registry.ModEnchantmentKey;
import com.carter.newenchants.registry.ModEnchantments;
import com.carter.newenchants.registry.ModParticles;
import com.carter.newenchants.system.projectile.EnchantProjectile;
import com.carter.newenchants.system.state.CooldownMap;
import com.carter.newenchants.system.state.TimestampMap;
import com.carter.newenchants.system.state.TrackedValueStore;
import com.carter.newenchants.system.util.ParticleUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RangedEventHooks {
    private static final CooldownMap COOLDOWNS = new CooldownMap();
    private static final TrackedValueStore VALUES = new TrackedValueStore();
    private static final TimestampMap TIMESTAMPS = new TimestampMap();
    private static final Map<UUID, Map<UUID, Long>> UMBRAL_MARKS = new HashMap<>();
    private static final Map<UUID, AbyssalWhirlpool> ABYSSAL_STACKS = new HashMap<>();

    private RangedEventHooks() {}

    public static void onBowReleased(LivingEntity user, ItemStack stack, World world, int remainingUseTicks) {
        if (world.isClient) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) world;
        handleStarfallDraw(user, stack, serverWorld, remainingUseTicks);
    }

    private static void handleStarfallDraw(LivingEntity user, ItemStack stack, ServerWorld world, int remainingUseTicks) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.STARFALL_DRAW), stack);
        if (level <= 0) {
            return;
        }
        int usedTicks = stack.getMaxUseTime() - remainingUseTicks;
        if (BowItem.getPullProgress(usedTicks) < 1.0f) {
            return;
        }
        if (COOLDOWNS.ready(user, "starfall", world.getTime(), Math.max(20, 60 - level * 10))) {
            ParticleUtil.burst(world, ParticleTypes.FALLING_OBSIDIAN_TEAR, user.getPos().add(0, 1, 0), 30, 0.3, 0.02);
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.4f);
            if (user instanceof PlayerEntity player) {
                VALUES.set(player, "starfall_pending", level);
            }
        }
    }

    public static void onCrossbowReleased(LivingEntity user, ItemStack stack, World world) {
        if (world.isClient) {
            return;
        }
        ServerWorld server = (ServerWorld) world;
        handleAetherSalvo(user, stack, server);
        handleSiegeRam(user, stack, server);
        handleSlipstream(user, stack, server);
    }

    private static void handleAetherSalvo(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.AETHER_SALVO), stack);
        if (level <= 0 || user.isOnGround()) {
            return;
        }
        int cooldown = switch (level) {
            case 1 -> 160;
            case 2 -> 120;
            default -> 100;
        };
        if (!COOLDOWNS.ready(user, "aether_salvo", world.getTime(), cooldown)) {
            return;
        }
        int bolts = level == 1 ? 1 : level == 2 ? 2 : 3;
        for (int i = 0; i < bolts; i++) {
            boolean finalBolt = level >= 3 && i == bolts - 1;
            spawnAetherBolt(user, world, level, finalBolt, i, bolts);
        }
        ParticleUtil.burst(world, ParticleTypes.END_ROD, user.getPos(), 20 + level * 5, 0.3, 0.01);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 0.7f, 1.5f);
    }

    private static void handleSiegeRam(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.SIEGE_RAM), stack);
        if (level <= 0) {
            return;
        }
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.5f, 0.8f + level * 0.1f);
    }

    private static void handleSlipstream(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.SLIPSTREAM_REPEATER), stack);
        if (level <= 0) {
            return;
        }
        int hasteDuration = 60 + level * 20;
        int hasteAmplifier = Math.max(0, level - 1);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, hasteDuration, hasteAmplifier, false, false));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40 + level * 10, 0, false, false));
        ParticleUtil.burst(world, ParticleTypes.CLOUD, user.getPos(), 20 + level * 4, 0.4, 0.01);
        if (user instanceof PlayerEntity player) {
            VALUES.set(player, "slipstream_ticks", hasteDuration);
            VALUES.set(player, "slipstream_level", level);
            boolean hasFirework = hasChargedFirework(stack);
            if (hasFirework) {
                VALUES.set(player, "slipstream_pending_firework", level);
            }
        }
    }

    public static void onTridentReleased(LivingEntity user, ItemStack stack, World world) {
        if (world.isClient) {
            return;
        }
        ServerWorld server = (ServerWorld) world;
        handleAbyssalChurn(user, stack, server);
        handleStormAnchor(user, stack, server);
        handleLeviathanCall(user, stack, server);
    }

    private static void handleAbyssalChurn(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.ABYSSAL_CHURN), stack);
        if (level <= 0) {
            return;
        }
        ParticleUtil.burst(world, ParticleTypes.BUBBLE_COLUMN_UP, user.getPos(), 20, 0.3, 0.02);
    }

    private static void handleStormAnchor(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.STORM_ANCHOR), stack);
        if (level <= 0) {
            return;
        }
        if (!user.isTouchingWaterOrRain()) {
            return;
        }
        ParticleUtil.burst(world, ParticleTypes.ELECTRIC_SPARK, user.getPos(), 20, 0.2, 0);
        if (user instanceof PlayerEntity player) {
            VALUES.set(player, "storm_anchor_pending", level);
        }
    }

    private static void handleLeviathanCall(LivingEntity user, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.LEVIATHAN_CALL), stack);
        if (level <= 0) {
            return;
        }
        if (EnchantmentHelper.getLevel(Enchantments.RIPTIDE, stack) <= 0 || !user.isWet()) {
            return;
        }
        int dolphinsDuration = switch (level) {
            case 1 -> 60;
            case 2 -> 100;
            default -> 140;
        };
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, dolphinsDuration, 0));
        if (user.getHealth() < user.getMaxHealth()) {
            user.heal(2f);
        }
        if (level >= 2) {
            int conduitDuration = level == 2 ? 80 : 120;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, conduitDuration, 0));
        }
        ParticleUtil.burst(world, ParticleTypes.NAUTILUS, user.getPos(), 24 + level * 6, 0.4, 0.01);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.9f, 1.1f);
        if (level >= 3) {
            summonGuardianSpirits(world, user);
        }
    }

    public static void onFishingCast(PlayerEntity player, ItemStack stack, World world) {
        if (world.isClient) {
            return;
        }
        handleTidalLure(player, stack, (ServerWorld) world);
        handleEchoLine(player, stack, (ServerWorld) world);
    }

    private static void handleTidalLure(PlayerEntity player, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.TIDAL_LURE), stack);
        if (level <= 0) {
            return;
        }
        ParticleUtil.burst(world, ParticleTypes.BUBBLE, player.getPos(), 20, 0.4, 0.01);
    }

    private static void handleEchoLine(PlayerEntity player, ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.ECHO_LINE), stack);
        if (level <= 0) {
            return;
        }
        ParticleUtil.burst(world, ModParticles.VOID_RIFT, player.getPos(), 15, 0.3, 0.01);
    }

    public static void onProjectileOwnerAssigned(PersistentProjectileEntity projectile) {
        if (projectile.getWorld().isClient()) {
            return;
        }
        if (!(projectile.getOwner() instanceof PlayerEntity player)) {
            return;
        }
        if (projectile instanceof TridentEntity) {
            markStormAnchor(player, projectile);
        }
        if (projectile.isShotFromCrossbow()) {
            ItemStack crossbow = getCrossbowStack(player);
            if (crossbow.isEmpty()) {
                return;
            }
            markSiegeRam(crossbow, projectile);
        } else {
            ItemStack bow = getBowStack(player);
            if (bow.isEmpty()) {
                return;
            }
            applyTempestVolley(player, bow, projectile);
            markVerdantTide(bow, projectile);
            markUmbralPin(bow, projectile);
            markStarfallDraw(player, projectile);
        }
    }

    public static void tickLiving(LivingEntity living, ServerWorld world, long time) {
        tickAbyssalWhirlpool(living, world, time);
    }

    public static void onProjectileBlockHit(PersistentProjectileEntity projectile, BlockHitResult hitResult) {
        if (projectile.getWorld().isClient()) {
            return;
        }
        ServerWorld world = (ServerWorld) projectile.getWorld();
        Vec3d center = Vec3d.ofCenter(hitResult.getBlockPos().offset(hitResult.getSide()));
        int verdant = ((EnchantProjectile) projectile).newenchants$getVerdantLevel();
        if (verdant > 0) {
            ((EnchantProjectile) projectile).newenchants$setVerdantLevel(0);
            triggerVerdantSnare(world, projectile, center, verdant);
        }
        int starfall = ((EnchantProjectile) projectile).newenchants$getStarfallLevel();
        if (starfall > 0 && projectile.getOwner() instanceof PlayerEntity owner) {
            ((EnchantProjectile) projectile).newenchants$setStarfallLevel(0);
            triggerStarfallShard(world, owner, center, starfall);
        }
        int aether = ((EnchantProjectile) projectile).newenchants$getAetherLevel();
        if (aether > 0) {
            ((EnchantProjectile) projectile).newenchants$setAetherLevel(0);
            projectile.discard();
        }
        ((EnchantProjectile) projectile).newenchants$setSiegeLevel(0);
        ((EnchantProjectile) projectile).newenchants$setUmbralLevel(0);
        triggerStormAnchorStrike(world, projectile, center, null);
    }

    public static void onProjectileEntityHit(PersistentProjectileEntity projectile, EntityHitResult hitResult) {
        if (projectile.getWorld().isClient()) {
            return;
        }
        ServerWorld world = (ServerWorld) projectile.getWorld();
        Entity hitEntity = hitResult.getEntity();
        int verdant = ((EnchantProjectile) projectile).newenchants$getVerdantLevel();
        if (verdant > 0) {
            ((EnchantProjectile) projectile).newenchants$setVerdantLevel(0);
            triggerVerdantSnare(world, projectile, hitResult.getPos(), verdant);
        }
        int umbral = ((EnchantProjectile) projectile).newenchants$getUmbralLevel();
        if (umbral > 0 && projectile.getOwner() instanceof PlayerEntity owner && hitEntity instanceof LivingEntity target) {
            handleUmbralPin(owner, target, umbral, world);
            ((EnchantProjectile) projectile).newenchants$setUmbralLevel(0);
        }
        int starfall = ((EnchantProjectile) projectile).newenchants$getStarfallLevel();
        if (starfall > 0 && projectile.getOwner() instanceof PlayerEntity owner) {
            ((EnchantProjectile) projectile).newenchants$setStarfallLevel(0);
            triggerStarfallShard(world, owner, hitResult.getPos(), starfall);
        }
        int aether = ((EnchantProjectile) projectile).newenchants$getAetherLevel();
        if (aether > 0 && hitEntity instanceof LivingEntity victim) {
            LivingEntity owner = projectile.getOwner() instanceof LivingEntity living ? living : null;
            float damage = 2f;
            if (owner != null) {
                victim.damage(world.getDamageSources().indirectMagic(projectile, owner), damage);
            } else {
                victim.damage(world.getDamageSources().magic(), damage);
            }
            if (((EnchantProjectile) projectile).newenchants$isAetherFinal()) {
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 0));
            }
            ParticleUtil.burst(world, ParticleTypes.END_ROD, victim.getPos().add(0, victim.getHeight() * 0.5, 0), 16, 0.1, 0.01);
            ((EnchantProjectile) projectile).newenchants$setAetherLevel(0);
            projectile.discard();
        }
        int siege = ((EnchantProjectile) projectile).newenchants$getSiegeLevel();
        if (siege > 0 && hitEntity instanceof LivingEntity target) {
            applySiegeRam(world, projectile, target, siege);
            ((EnchantProjectile) projectile).newenchants$setSiegeLevel(0);
        }
        if (projectile instanceof TridentEntity trident && hitEntity instanceof LivingEntity target) {
            handleAbyssalHit(trident, target, world);
        }
        triggerStormAnchorStrike(world, projectile, hitResult.getPos(), hitResult.getEntity());
    }

    private static void applyTempestVolley(PlayerEntity player, ItemStack bow, PersistentProjectileEntity projectile) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.TEMPEST_VOLLEY), bow);
        if (level <= 0 || !(projectile.getWorld() instanceof ServerWorld world)) {
            return;
        }
        long time = world.getTime();
        long last = TIMESTAMPS.get(player, "tempest_last_shot");
        int maxStacks = switch (level) {
            case 1 -> 3;
            case 2 -> 4;
            default -> 5;
        };
        int stack;
        if (last == 0 || time - last > 40) {
            stack = 1;
        } else {
            stack = (int) Math.min(maxStacks, VALUES.get(player, "tempest_stack") + 1);
        }
        TIMESTAMPS.mark(player, "tempest_last_shot", time);
        VALUES.set(player, "tempest_stack", stack);

        float damagePercent = switch (level) {
            case 1 -> 0.05f;
            case 2 -> 0.08f;
            default -> 0.10f;
        };
        float multiplier = 1f + (damagePercent * stack);
        projectile.setDamage(projectile.getDamage() * multiplier);
        ((EnchantProjectile) projectile).newenchants$setTempestBonus(multiplier);

        int hasteAmplifier = Math.max(0, stack - 1) / 2;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, hasteAmplifier, false, false));
        ParticleUtil.burst(world, ModParticles.SOLAR_SPARK, player.getPos().add(0, 1.2, 0), 16 + stack * 4, 0.2, 0);

        if (stack >= maxStacks) {
            releaseTempestGust(player, world, level);
            VALUES.set(player, "tempest_stack", 0);
            TIMESTAMPS.mark(player, "tempest_last_shot", time);
        }
    }

    private static void releaseTempestGust(PlayerEntity player, ServerWorld world, int level) {
        if (level < 2) {
            return;
        }
        double radius = 3 + level;
        Box zone = player.getBoundingBox().expand(radius);
        world.getEntitiesByClass(LivingEntity.class, zone, entity -> entity != player)
                .forEach(entity -> {
                    float gustDamage = level == 2 ? 2f : 3f;
                    entity.damage(world.getDamageSources().playerAttack(player), gustDamage);
                    Vec3d offset = entity.getPos().subtract(player.getPos());
                    if (offset.lengthSquared() > 0.001) {
                        offset = offset.normalize();
                    }
                    Vec3d push = offset.multiply(0.4 + level * 0.05);
                    entity.addVelocity(push.x, 0.2, push.z);
                    if (level >= 3) {
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 1));
                    }
                });
        ParticleUtil.burst(world, ParticleTypes.GUST, player.getPos().add(0, 1, 0), 28, 0.4, 0.02);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 0.85f);
    }

    private static void markVerdantTide(ItemStack bow, PersistentProjectileEntity projectile) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.VERDANT_TIDE), bow);
        if (level <= 0) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setVerdantLevel(level);
    }

    private static void markUmbralPin(ItemStack bow, PersistentProjectileEntity projectile) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.UMBRAL_PIN), bow);
        if (level <= 0) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setUmbralLevel(level);
    }

    private static void markStarfallDraw(PlayerEntity player, PersistentProjectileEntity projectile) {
        double pending = VALUES.get(player, "starfall_pending");
        if (pending <= 0) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setStarfallLevel((int) pending);
        VALUES.set(player, "starfall_pending", 0);
    }

    private static void markStormAnchor(PlayerEntity player, PersistentProjectileEntity projectile) {
        double pending = VALUES.get(player, "storm_anchor_pending");
        if (pending <= 0 || !(projectile instanceof TridentEntity)) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setStormLevel((int) pending);
        VALUES.set(player, "storm_anchor_pending", 0);
    }

    private static void markSiegeRam(ItemStack crossbow, PersistentProjectileEntity projectile) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.SIEGE_RAM), crossbow);
        if (level <= 0) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setSiegeLevel(level);
    }

    private static boolean hasChargedFirework(ItemStack crossbow) {
        if (!crossbow.hasNbt()) {
            return false;
        }
        NbtCompound nbt = crossbow.getNbt();
        if (nbt == null || !nbt.contains("ChargedProjectiles", NbtElement.LIST_TYPE)) {
            return false;
        }
        NbtList projectiles = nbt.getList("ChargedProjectiles", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < projectiles.size(); i++) {
            NbtCompound tag = projectiles.getCompound(i);
            ItemStack loaded = ItemStack.fromNbt(tag);
            if (loaded.isOf(Items.FIREWORK_ROCKET)) {
                return true;
            }
        }
        return false;
    }

    private static void triggerVerdantSnare(ServerWorld world, PersistentProjectileEntity projectile, Vec3d center, int level) {
        float radius = switch (level) {
            case 1 -> 3f;
            case 2 -> 4f;
            default -> 5f;
        };
        int slowDuration = 60 + level * 10;
        int slowAmplifier = level - 1;
        ParticleUtil.burst(world, ModParticles.VERDANT_RING, center, 40 + level * 10, radius * 0.2, 0.02);
        Box area = new Box(center, center).expand(radius, 1.5, radius);
        world.getEntitiesByClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != projectile.getOwner())
                .forEach(entity -> {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, Math.max(0, slowAmplifier)));
                    if (level >= 2 && entity.isUndead()) {
                        float damage = level == 2 ? 1.5f : 2.5f;
                        entity.damage(world.getDamageSources().magic(), damage);
                    }
                    if (level >= 3 && entity instanceof HostileEntity) {
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
                    }
                });
    }

    private static void triggerStarfallShard(ServerWorld world, PlayerEntity owner, Vec3d center, int level) {
        float radius = switch (level) {
            case 1 -> 1.5f;
            case 2 -> 2.2f;
            default -> 3f;
        };
        float damage = switch (level) {
            case 1 -> 2f;
            case 2 -> 3f;
            default -> 4f;
        };
        ParticleUtil.burst(world, ParticleTypes.FALLING_OBSIDIAN_TEAR, center.add(0, 4, 0), 30, 0.2, 0.02);
        ParticleUtil.burst(world, ModParticles.SOLAR_SPARK, center, 24, radius * 0.2, 0.01);
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 1.3f);
        Box box = new Box(center, center).expand(radius, 1.5, radius);
        world.getEntitiesByClass(LivingEntity.class, box, entity -> entity.isAlive() && entity != owner)
                .forEach(entity -> entity.damage(world.getDamageSources().magic(), damage));
        if (level >= 3) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100, 0, false, false));
        }
    }

    private static void handleUmbralPin(PlayerEntity owner, LivingEntity target, int level, ServerWorld world) {
        long now = world.getTime();
        long expiry = getMarkExpiry(owner, target);
        if (expiry > now) {
            clearMark(owner, target);
            detonateUmbralMark(owner, target, level, world);
        } else {
            applyUmbralMark(owner, target, level, world, now);
        }
    }

    private static void detonateUmbralMark(PlayerEntity owner, LivingEntity target, int level, ServerWorld world) {
        int rootTicks = switch (level) {
            case 1 -> 16;
            case 2 -> 24;
            default -> 32;
        };
        float bonusDamage = switch (level) {
            case 1 -> 4f;
            case 2 -> 6f;
            default -> 8f;
        };
        target.damage(world.getDamageSources().magic(), bonusDamage);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 6));
        if (level >= 3) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, 0));
        }
        ParticleUtil.burst(world, ParticleTypes.SCULK_SOUL, target.getPos().add(0, target.getHeight() * 0.5, 0), 28, 0.25, 0.02);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING_STOP, SoundCategory.PLAYERS, 0.8f, 0.6f + level * 0.1f);
    }

    private static void applyUmbralMark(PlayerEntity owner, LivingEntity target, int level, ServerWorld world, long now) {
        long duration = switch (level) {
            case 1 -> 120;
            case 2 -> 160;
            default -> 200;
        };
        setMark(owner, target, now + duration);
        if (level >= 2) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, (int) duration, 0, false, false));
        }
        ParticleUtil.burst(world, ParticleTypes.SCULK_SOUL, target.getPos().add(0, target.getHeight() * 0.5, 0), 18, 0.15, 0.01);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, 0.6f, 1.2f);
    }

    private static long getMarkExpiry(PlayerEntity owner, LivingEntity target) {
        Map<UUID, Long> targetMap = UMBRAL_MARKS.get(owner.getUuid());
        if (targetMap == null) {
            return 0;
        }
        return targetMap.getOrDefault(target.getUuid(), 0L);
    }

    private static void setMark(PlayerEntity owner, LivingEntity target, long expires) {
        UMBRAL_MARKS.computeIfAbsent(owner.getUuid(), uuid -> new HashMap<>()).put(target.getUuid(), expires);
    }

    private static void clearMark(PlayerEntity owner, LivingEntity target) {
        Map<UUID, Long> targetMap = UMBRAL_MARKS.get(owner.getUuid());
        if (targetMap != null) {
            targetMap.remove(target.getUuid());
            if (targetMap.isEmpty()) {
                UMBRAL_MARKS.remove(owner.getUuid());
            }
        }
    }

    private static ItemStack getBowStack(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof BowItem) {
            return main;
        }
        ItemStack off = player.getOffHandStack();
        if (off.getItem() instanceof BowItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getCrossbowStack(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof CrossbowItem) {
            return main;
        }
        ItemStack off = player.getOffHandStack();
        if (off.getItem() instanceof CrossbowItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static void spawnAetherBolt(LivingEntity user, ServerWorld world, int level, boolean finalBolt, int index, int total) {
        SpectralArrowEntity bolt = new SpectralArrowEntity(world, user);
        bolt.setDamage(0);
        bolt.setCritical(true);
        bolt.setPickupPermission(PickupPermission.CREATIVE_ONLY);
        bolt.setNoGravity(true);
        bolt.setShotFromCrossbow(true);
        Vec3d look = user.getRotationVec(1f);
        double verticalOffset = total == 1 ? 0 : (index - (total - 1) / 2.0) * 0.04;
        Vec3d velocity = new Vec3d(look.x, look.y + verticalOffset, look.z).normalize();
        bolt.setVelocity(velocity.x, velocity.y, velocity.z, 3.4f, 0);
        ((EnchantProjectile) bolt).newenchants$setAetherLevel(level);
        ((EnchantProjectile) bolt).newenchants$setAetherFinal(finalBolt);
        world.spawnEntity(bolt);
    }

    private static void handleAbyssalHit(TridentEntity trident, LivingEntity target, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.ABYSSAL_CHURN), trident.asItemStack());
        if (level <= 0) {
            return;
        }
        LivingEntity owner = trident.getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) {
            return;
        }
        if (!owner.isTouchingWater() || !target.isTouchingWater()) {
            return;
        }
        addAbyssalStack(owner, target, level, world);
    }

    private static void addAbyssalStack(LivingEntity owner, LivingEntity target, int level, ServerWorld world) {
        AbyssalWhirlpool data = ABYSSAL_STACKS.computeIfAbsent(target.getUuid(), uuid -> new AbyssalWhirlpool());
        data.stacks = Math.min(3, data.stacks + 1);
        data.level = Math.max(level, data.level);
        data.ownerId = owner.getUuid();
        data.expiresAt = world.getTime() + 80;
        if (data.lastDamageTick == 0) {
            data.lastDamageTick = world.getTime();
        }
        ParticleUtil.burst(world, ParticleTypes.BUBBLE_COLUMN_UP, target.getPos(), 12 + data.stacks * 4, 0.3, 0.02);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.5f, 0.9f + 0.05f * data.stacks);
    }

    private static void tickAbyssalWhirlpool(LivingEntity target, ServerWorld world, long time) {
        AbyssalWhirlpool data = ABYSSAL_STACKS.get(target.getUuid());
        if (data == null) {
            return;
        }
        if (!target.isAlive() || !target.isTouchingWater() || time >= data.expiresAt || data.stacks <= 0) {
            ABYSSAL_STACKS.remove(target.getUuid());
            return;
        }
        int baseSlow = switch (data.level) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 20;
        };
        int totalSlow = baseSlow * data.stacks;
        int amplifier = computeSlowAmplifier(totalSlow);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, amplifier, false, false, true));
        if (data.level >= 2 && time - data.lastDamageTick >= 20) {
            target.damage(world.getDamageSources().drown(), 1f);
            data.lastDamageTick = time;
        }
        if (data.level >= 3 && data.ownerId != null) {
            Entity owner = world.getEntity(data.ownerId);
            if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                Vec3d delta = livingOwner.getPos().subtract(target.getPos());
                if (delta.lengthSquared() > 0.0001) {
                    Vec3d pull = delta.normalize().multiply(0.3);
                    target.addVelocity(pull.x, pull.y * 0.15, pull.z);
                }
            }
        }
        if (time % 5 == 0) {
            ParticleUtil.burst(world, ParticleTypes.BUBBLE, target.getPos(), 6 + data.stacks * 2, 0.2, 0.01);
        }
    }

    private static int computeSlowAmplifier(int percent) {
        return Math.max(0, Math.round(percent / 15f) - 1);
    }

    private static void triggerStormAnchorStrike(ServerWorld world, PersistentProjectileEntity projectile, Vec3d impact, Entity primaryTarget) {
        int level = ((EnchantProjectile) projectile).newenchants$getStormLevel();
        if (level <= 0 || !(projectile instanceof TridentEntity)) {
            return;
        }
        if (EnchantmentHelper.getLevel(Enchantments.CHANNELING, projectile.asItemStack()) <= 0) {
            return;
        }
        Vec3d center = impact != null ? impact : projectile.getPos();
        if (!canChannelLightning(world, center)) {
            return;
        }
        LivingEntity owner = projectile.getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) {
            return;
        }
        ((EnchantProjectile) projectile).newenchants$setStormLevel(0);
        int resistanceDuration = level == 1 ? 60 : 80;
        int fireDuration = level == 1 ? 60 : 80;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistanceDuration, Math.max(0, level - 1)));
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireDuration, 0));
        ParticleUtil.burst(world, ParticleTypes.ELECTRIC_SPARK, center, 30 + level * 10, 0.25, 0.02);
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.9f, 0.5f + level * 0.2f);
        if (level >= 2) {
            chainStormDamage(world, center, owner, primaryTarget);
        }
    }

    private static boolean canChannelLightning(ServerWorld world, Vec3d impact) {
        if (!world.isThundering() || world.getRegistryKey() == World.NETHER) {
            return false;
        }
        BlockPos pos = BlockPos.ofFloored(impact);
        return world.isSkyVisible(pos);
    }

    private static void chainStormDamage(ServerWorld world, Vec3d origin, LivingEntity owner, Entity primaryTarget) {
        double radius = 4.5;
        Box zone = new Box(origin, origin).expand(radius, 2.5, radius);
        world.getEntitiesByClass(LivingEntity.class, zone, entity -> entity.isAlive() && entity != owner && entity != primaryTarget && isStormConductive(entity))
                .forEach(entity -> {
                    entity.damage(world.getDamageSources().lightningBolt(), 2f);
                    ParticleUtil.burst(world, ParticleTypes.ELECTRIC_SPARK, entity.getPos().add(0, entity.getHeight() * 0.5, 0), 12, 0.15, 0.01);
                    world.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_LIGHTNING_ROD_ZAP, SoundCategory.PLAYERS, 0.5f, 1.2f);
                });
    }

    private static boolean isStormConductive(LivingEntity entity) {
        return entity.isTouchingWater() || entity.isWet() || entity.isSubmergedInWater();
    }

    private static void summonGuardianSpirits(ServerWorld world, LivingEntity user) {
        Box zone = user.getBoundingBox().expand(8, 4, 8);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, zone,
                entity -> entity.isAlive() && entity != user && entity instanceof HostileEntity);
        targets.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(user)));
        int strikes = 0;
        for (LivingEntity target : targets) {
            guardianSpiritStrike(world, user, target);
            strikes++;
            if (strikes >= 2) {
                break;
            }
        }
        if (strikes == 0) {
            ParticleUtil.burst(world, ParticleTypes.NAUTILUS, user.getPos().add(0, 1, 0), 18, 0.25, 0.01);
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_DOLPHIN_AMBIENT, SoundCategory.PLAYERS, 0.7f, 1.3f);
        }
    }

    private static void guardianSpiritStrike(ServerWorld world, LivingEntity owner, LivingEntity target) {
        Vec3d start = owner.getPos().add(0, owner.getStandingEyeHeight() * 0.6, 0);
        Vec3d end = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d diff = end.subtract(start);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = start.add(diff.multiply(t));
            world.spawnParticles(ParticleTypes.NAUTILUS, point.x, point.y, point.z, 2, 0.02, 0.02, 0.02, 0.01);
        }
        target.damage(world.getDamageSources().indirectMagic(owner, owner), 2f);
        ParticleUtil.burst(world, ParticleTypes.BUBBLE, target.getPos().add(0, target.getHeight() * 0.5, 0), 14, 0.15, 0.01);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    public static void tickPlayer(PlayerEntity player, ServerWorld world) {
        double ticks = VALUES.get(player, "slipstream_ticks");
        if (ticks > 0) {
            int level = (int) VALUES.get(player, "slipstream_level");
            int hasteAmplifier = Math.max(0, level - 1);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 10, hasteAmplifier, false, false, true));
            VALUES.add(player, "slipstream_ticks", -1, 0, 200);
        }
    }

    public static void onFireworkLaunched(PlayerEntity player) {
        double pending = VALUES.get(player, "slipstream_pending_firework");
        if (pending <= 0) {
            return;
        }
        int level = (int) pending;
        VALUES.set(player, "slipstream_pending_firework", 0);
        if (level >= 3) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false));
        }
        if (level >= 2) {
            float chance = level == 2 ? 0.05f : 0.10f;
            if (player.getRandom().nextFloat() < chance) {
                rewardFirework(player);
            }
        }
    }

    private static void rewardFirework(PlayerEntity player) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        if (!player.getInventory().insertStack(rocket)) {
            player.dropItem(rocket, false);
        }
        if (player.getWorld() instanceof ServerWorld world) {
            ParticleUtil.burst(world, ParticleTypes.END_ROD, player.getPos().add(0, 1, 0), 10, 0.2, 0.01);
        }
    }

    private static final class AbyssalWhirlpool {
        int stacks;
        int level;
        UUID ownerId;
        long expiresAt;
        long lastDamageTick;
    }

    private static void applySiegeRam(ServerWorld world, PersistentProjectileEntity projectile, LivingEntity target, int level) {
        boolean golem = target instanceof GolemEntity;
        boolean shielded = target instanceof PlayerEntity defender && defender.isBlocking();
        if (!golem && !shielded) {
            return;
        }
        float bonusDamage = switch (level) {
            case 1 -> 3f;
            case 2 -> 4.5f;
            default -> 6f;
        };
        target.damage(world.getDamageSources().projectile(projectile, projectile.getOwner()), bonusDamage);
        if (shielded && target instanceof PlayerEntity defender) {
            defender.disableShield(true);
            if (!defender.getWorld().isClient) {
                defender.getItemCooldownManager().set(defender.getActiveItem().getItem(), 60 - level * 10);
            }
        }
        ParticleUtil.burst(world, ParticleTypes.CRIT, target.getPos().add(0, target.getHeight() * 0.5, 0), 20, 0.2, 0.01);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.6f, 0.6f + level * 0.05f);
    }
}
