package com.carter.newenchants.system;

import com.carter.newenchants.registry.ModEnchantmentKey;
import com.carter.newenchants.registry.ModEnchantments;
import com.carter.newenchants.registry.ModParticles;
import com.carter.newenchants.system.helpers.EnchantmentUtil;
import com.carter.newenchants.system.state.CooldownMap;
import com.carter.newenchants.system.state.TargetCooldownMap;
import com.carter.newenchants.system.state.TimestampMap;
import com.carter.newenchants.system.state.TrackedValueStore;
import com.carter.newenchants.system.util.ParticleUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public final class EnchantmentEvents {
    private static final CooldownMap COOLDOWNS = new CooldownMap();
    private static final TargetCooldownMap TARGET_COOLDOWNS = new TargetCooldownMap();
    private static final TrackedValueStore VALUES = new TrackedValueStore();
    private static final TimestampMap TIMESTAMPS = new TimestampMap();
    private static final Set<EntityType<?>> WITHER_PRIORITY = Set.of(
            EntityType.WITHER,
            EntityType.WITHER_SKELETON,
            EntityType.ENDER_DRAGON,
            EntityType.WARDEN
    );
        private static final StatusEffect[] MINDWARD_STATUSES = {
            StatusEffects.BLINDNESS,
            StatusEffects.DARKNESS,
            StatusEffects.NAUSEA
        };

    private EnchantmentEvents() {}

    public static void init() {
        AttackEntityCallback.EVENT.register(EnchantmentEvents::onAttack);
        UseEntityCallback.EVENT.register(EnchantmentEvents::onUseEntity);
        UseBlockCallback.EVENT.register(EnchantmentEvents::onUseBlock);
        PlayerBlockBreakEvents.AFTER.register(EnchantmentEvents::afterBlockBreak);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof PlayerEntity player) {
                handleKill(player, entity);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player) {
                handleSolarRiposteGuard(player, source, amount);
            }
            return true;
        });
        ServerTickEvents.END_ENTITY_TICK.register(EnchantmentEvents::tickEntity);
    }

    private static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof LivingEntity target) || world.isClient) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        long time = world.getTime();

        handleChronoblade(player, target, stack, world, time);
        handleBloodOath(player, target, stack, world, time);
        handleVoidbreaker(player, target, stack, world);
        handleRadiantExecution(player, target, stack, world);
        handlePhantomGuillotine(player, target, world, time);
        markRiftedLungeTarget(player, target, time);
        handleWitherSeverance(player, target, world);
        handleTitanHew(player, target, world, time);
        handleThunderChop(player, target, world);
        handleImpactBloom(player, target, world);
        handleShockLancer(player, target, world);
        handleGroveShatterCombat(player, target, world);

        return ActionResult.PASS;
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClient || !(entity instanceof LivingEntity target)) {
            return ActionResult.PASS;
        }
        return handleRiftedBlink(player, target, world);
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.PASS;
        }
        return handleBloomBinder(player, (ServerWorld) world, hit);
    }

    private static void handleChronoblade(PlayerEntity player, LivingEntity target, ItemStack stack, World world, long currentTick) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.CHRONOBLADE_EDGE), stack);
        if (level <= 0) {
            return;
        }
        long last = TIMESTAMPS.get(player, "chronoblade_last");
        boolean chained = currentTick - last <= 80;
        TIMESTAMPS.mark(player, "chronoblade_last", currentTick);
        if (!chained) {
            VALUES.set(player, "chronoblade_stack", 0);
        }
        int maxStacks = switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            default -> 3;
        };
        double next = VALUES.add(player, "chronoblade_stack", 1, 0, maxStacks);
        float bonus = switch (level) {
            case 1 -> 0.06f;
            case 2 -> 0.10f;
            default -> 0.14f;
        };
        float extraDamage = bonus * (float) next * 6f;
        target.damage(world.getDamageSources().playerAttack(player), extraDamage);
        int slowDuration = level == 1 ? 20 : level == 2 ? 30 : 30;
        int slowAmplifier = level >= 3 ? 1 : 0;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, slowAmplifier));
        if (level >= 2) {
            int bleedDuration = level == 2 ? 40 : 60;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, bleedDuration, level - 1));
        }
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ModParticles.SOLAR_SPARK, target.getPos(), 24, 0.3, 0.05);
        }
    }

    private static void handleBloodOath(PlayerEntity player, LivingEntity target, ItemStack stack, World world, long tick) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.BLOOD_OATH), stack);
        if (level <= 0) {
            return;
        }
        double threshold = level == 1 ? 0.4 : 0.6;
        if (target.getHealth() / target.getMaxHealth() > threshold) {
            return;
        }
        int cooldown = level == 1 ? 100 : 80;
        if (!TARGET_COOLDOWNS.ready(player, target, "blood_oath", tick, cooldown)) {
            return;
        }
        float damage = level == 1 ? 2f : 3f;
        DamageSource source = world.getDamageSources().magic();
        target.damage(source, damage);
        player.heal(level == 1 ? 2f : 3f);
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ParticleTypes.HEART, target.getPos(), 10, 0.2, 0.01);
        }
    }

    private static void handleVoidbreaker(PlayerEntity player, LivingEntity target, ItemStack stack, World world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.VOIDBREAKER), stack);
        if (level <= 0) {
            return;
        }
        boolean shieldedHit = target instanceof PlayerEntity tp && tp.isBlocking();
        boolean resistance = target.hasStatusEffect(StatusEffects.RESISTANCE) || target.getAbsorptionAmount() > 0;
        if (!shieldedHit && !resistance) {
            return;
        }
        float percent = switch (level) {
            case 1 -> 0.25f;
            case 2 -> 0.35f;
            default -> 0.45f;
        };
        target.damage(world.getDamageSources().playerAttack(player), percent * 6f);
        if (target instanceof PlayerEntity shieldUser) {
            int disableTicks = switch (level) {
                case 1 -> 30;
                case 2 -> 50;
                default -> 70;
            };
            shieldUser.disableShield(true);
            shieldUser.getItemCooldownManager().set(shieldUser.getActiveItem().getItem(), disableTicks);
            if (level >= 3) {
                shieldUser.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0));
            }
        }
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ModParticles.VOID_RIFT, target.getPos(), 18, 0.4, 0.04);
        }
    }

    private static void handleRadiantExecution(PlayerEntity player, LivingEntity target, ItemStack stack, World world) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.RADIANT_EXECUTION), stack);
        if (level <= 0 || !target.isUndead()) {
            return;
        }
        float bonus = switch (level) {
            case 1 -> 0.15f;
            case 2 -> 0.25f;
            default -> 0.35f;
        };
        target.damage(world.getDamageSources().playerAttack(player), bonus * 6f);
    }

    private static void handlePhantomGuillotine(PlayerEntity player, LivingEntity target, World world, long currentTick) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.PHANTOM_GUILLOTINE));
        if (level <= 0) {
            return;
        }
        long window = TIMESTAMPS.get(player, "phantom_ready_until");
        if (window == 0 || currentTick > window) {
            return;
        }
        float slashDamage = switch (level) {
            case 1 -> 2f;
            case 2 -> 3f;
            default -> 4f;
        };
        target.damage(world.getDamageSources().magic(), slashDamage);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, level == 1 ? 10 : level == 2 ? 12 : 20, 0));
        if (target instanceof WardenEntity) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
        }
        TIMESTAMPS.mark(player, "phantom_ready_until", 0);
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ParticleTypes.GUST, player.getPos().add(0, 1, 0), 24, 0.2, 0.03);
        }
    }

    private static void markRiftedLungeTarget(PlayerEntity player, LivingEntity target, long tick) {
        TIMESTAMPS.mark(player, "rift_target_id", target.getId());
        TIMESTAMPS.mark(player, "rift_target_time", tick);
    }

    private static ActionResult handleRiftedBlink(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.RIFTED_LUNGE));
        if (level <= 0) {
            return ActionResult.PASS;
        }
        long lastId = TIMESTAMPS.get(player, "rift_target_id");
        long lastTime = TIMESTAMPS.get(player, "rift_target_time");
        if (lastId != target.getId() || world.getTime() - lastTime > 60) {
            return ActionResult.PASS;
        }
        int cooldown = level == 1 ? 160 : 120;
        if (!COOLDOWNS.ready(player, "rift_blink", world.getTime(), cooldown)) {
            return ActionResult.PASS;
        }
        Vec3d look = player.getRotationVec(1f).normalize();
        double distance = level == 1 ? 2.0 : 3.0;
        Vec3d newPos = target.getPos().add(look.multiply(distance));
        player.requestTeleport(newPos.x, newPos.y, newPos.z);
        float damage = level == 1 ? 2f : 3f;
        target.damage(world.getDamageSources().playerAttack(player), damage);
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ModParticles.VOID_RIFT, player.getPos(), 28, 0.3, 0.04);
        }
        return ActionResult.SUCCESS;
    }

    private static void handleWitherSeverance(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.WITHER_SEVERANCE));
        if (level <= 0) {
            return;
        }
        if (!WITHER_PRIORITY.contains(target.getType())) {
            return;
        }
        float bonus = level == 1 ? 0.25f : 0.4f;
        target.damage(world.getDamageSources().playerAttack(player), bonus * 8f);
        player.removeStatusEffect(StatusEffects.WITHER);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60 + level * 20, level - 1));
        if (level >= 2 && COOLDOWNS.ready(player, "wither_reflect", world.getTime(), 240)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));
        }
    }

    private static void handleThunderChop(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.THUNDER_CHOP));
        if (level <= 0 || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!(player.getMainHandStack().getItem() instanceof AxeItem)) {
            return;
        }
        boolean wetCondition = player.isTouchingWaterOrRain() || target.isWet();
        boolean crit = player.getAttackCooldownProgress(0.5f) > 0.9f;
        boolean trigger = switch (level) {
            case 1 -> crit && wetCondition;
            case 2 -> wetCondition || (crit && world.getRandom().nextFloat() < 0.3f);
            default -> true;
        };
        if (!trigger || !COOLDOWNS.ready(player, "thunder_chop", world.getTime(), Math.max(40, 140 - level * 20))) {
            return;
        }
        serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 30 + level * 10, 0.4, 0.4, 0.4, 0);
        target.damage(world.getDamageSources().lightningBolt(), 4f + level);
        if (level >= 3) {
            serverWorld.getOtherEntities(player, target.getBoundingBox().expand(2), e -> e instanceof LivingEntity && e != target)
                    .stream().limit(2).forEach(area -> area.damage(world.getDamageSources().lightningBolt(), 4f));
        }
    }

    private static void handleTitanHew(PlayerEntity player, LivingEntity primary, World world, long tick) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.TITAN_HEW));
        if (level <= 0) {
            return;
        }
        long ready = TIMESTAMPS.get(player, "titan_ready_tick");
        if (ready == 0 || tick - ready > 40) {
            return;
        }
        float bonus = switch (level) {
            case 1 -> 0.25f;
            case 2 -> 0.35f;
            default -> 0.45f;
        };
        primary.damage(world.getDamageSources().playerAttack(player), bonus * 6f);
        float radius = 1f + level;
        List<Entity> cleaveTargets = world.getOtherEntities(player, player.getBoundingBox().expand(radius), e -> e instanceof LivingEntity && e != primary);
        for (Entity cleave : cleaveTargets) {
            ((LivingEntity) cleave).damage(world.getDamageSources().playerAttack(player), 2f + level);
        }
        TIMESTAMPS.mark(player, "titan_ready_tick", 0);
    }

    private static void handleWarcaller(PlayerEntity player, ServerWorld world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.WARCALLER));
        if (level <= 0) {
            return;
        }
        int cooldown = level == 1 ? 400 : 360;
        if (!COOLDOWNS.ready(player, "warcaller", world.getTime(), cooldown)) {
            return;
        }
        int duration = level == 1 ? 160 : 200;
        Box area = player.getBoundingBox().expand(8);
        world.getEntitiesByClass(PlayerEntity.class, area, mate -> mate.isTeammate(player) || mate == player)
                .forEach(ally -> {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, level - 1));
                    if (level >= 2) {
                        ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
                    }
                });
        world.playSound(null, player.getBlockPos(), SoundEvents.EVENT_RAID_HORN, SoundCategory.PLAYERS, 1f, 1f);
    }

    private static void handleImpactBloom(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.IMPACT_BLOOM));
        if (level <= 0 || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        long readyUntil = TIMESTAMPS.get(player, "impact_ready_until");
        if (readyUntil == 0) {
            return;
        }
        int cooldown = switch (level) {
            case 1 -> 160;
            case 2 -> 120;
            default -> 100;
        };
        if (!COOLDOWNS.ready(player, "impact_bloom", world.getTime(), cooldown)) {
            return;
        }
        TIMESTAMPS.mark(player, "impact_ready_until", 0);
        float mainDamage = switch (level) {
            case 1 -> 4f;
            case 2 -> 6f;
            default -> 8f;
        };
        float splashDamage = switch (level) {
            case 1 -> 4f;
            case 2 -> 6f;
            default -> 8f;
        };
        float radius = switch (level) {
            case 1 -> 3f;
            case 2 -> 4f;
            default -> 5f;
        };
        Vec3d center = target.getPos();
        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.x, center.y, center.z, 40, 0.6, 0.2, 0.6, 0.01);
        serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, radius * 0.2, 0, radius * 0.2, 0);
        target.damage(world.getDamageSources().playerAttack(player), mainDamage);
        Box area = target.getBoundingBox().expand(radius);
        for (Entity splash : world.getOtherEntities(player, area, e -> e instanceof LivingEntity && e != target)) {
            ((LivingEntity) splash).damage(world.getDamageSources().playerAttack(player), splashDamage);
        }
        if (level >= 3) {
            serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 25, 0.3, 0.2, 0.3, 0.02);
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(serverWorld, center.x, center.y, center.z);
            cloud.setRadius(2.5f);
            cloud.setDuration(40);
            cloud.setWaitTime(0);
            cloud.setRadiusOnUse(-0.05f);
            cloud.setRadiusGrowth(-0.02f);
            cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
            cloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 0));
            serverWorld.spawnEntity(cloud);
        }
    }

    private static void handleShockLancer(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.SHOCK_LANCER));
        if (level <= 0) {
            return;
        }
        long readyTick = TIMESTAMPS.get(player, "shock_ready_tick");
        if (readyTick == 0 || world.getTime() - readyTick > 40) {
            return;
        }
        float bonus = level == 1 ? 1.5f : 2.5f;
        target.damage(world.getDamageSources().playerAttack(player), (2f + level) + bonus);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, level - 1));
        TIMESTAMPS.mark(player, "shock_ready_tick", 0);
    }

    private static void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient) {
            return;
        }
        handleToolEnchantments((ServerWorld) world, player, pos, state);
        handleGroveShatter((ServerWorld) world, player, pos, state);
    }

    private static void handleToolEnchantments(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state) {
        int lode = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.LODE_HARMONIZER));
        if (lode > 0 && state.isIn(BlockTags.ORES)) {
            ParticleUtil.burst(world, ParticleTypes.END_ROD, Vec3d.ofCenter(pos), 12, 0.3, 0.01);
        }
        int terra = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.TERRA_SURGE));
        if (terra > 0 && state.isIn(BlockTags.DIRT)) {
            double stacks = VALUES.add(player, "terra_stacks", 1, 0, 20 + terra * 10);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, (int) (stacks / 20)));
        }
    }

    private static void handleGroveShatter(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.GROVE_SHATTER));
        if (level <= 0 || !state.isIn(BlockTags.LOGS)) {
            return;
        }
        int height = level == 1 ? 3 : 5;
        Direction up = Direction.UP;
        for (int i = 1; i < height; i++) {
            BlockPos next = pos.offset(up, i);
            BlockState nextState = world.getBlockState(next);
            if (!nextState.isIn(BlockTags.LOGS)) {
                break;
            }
            world.breakBlock(next, true, player);
        }
    }

    private static void handleGroveShatterCombat(PlayerEntity player, LivingEntity target, World world) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.GROVE_SHATTER));
        if (level <= 0) {
            return;
        }
        if (target instanceof ArmorStandEntity) {
            float woodBonus = level == 1 ? 0.4f : 0.6f;
            target.damage(world.getDamageSources().playerAttack(player), woodBonus * 6f);
        }
        if (target instanceof GolemEntity) {
            float golemBonus = level == 1 ? 0.2f : 0.35f;
            target.damage(world.getDamageSources().playerAttack(player), golemBonus * 6f);
        }
        if (level >= 2) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 6));
        }
    }

    private static void handleKill(PlayerEntity killer, LivingEntity victim) {
        World world = killer.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        int radiant = EnchantmentUtil.getHeldLevel(killer, ModEnchantments.get(ModEnchantmentKey.RADIANT_EXECUTION));
        if (radiant > 0 && victim.isUndead()) {
            double radius = 3 + Math.max(0, radiant - 1);
            float pulseDamage = switch (radiant) {
                case 1 -> 4f;
                case 2 -> 6f;
                default -> 8f;
            };
            serverWorld.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(radius), LivingEntity::isUndead)
                    .forEach(undead -> {
                        undead.damage(serverWorld.getDamageSources().magic(), pulseDamage);
                        if (radiant >= 2) {
                            undead.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
                        }
                        if (radiant >= 3) {
                            undead.setOnFireFor(4);
                        }
                    });
            if (radiant >= 3) {
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 0, false, false));
            }
            ParticleUtil.burst(serverWorld, ParticleTypes.END_ROD, victim.getPos(), 50, 0.4, 0.05);
        }
        handleWarcaller(killer, serverWorld);
    }

    private static void tickEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) entity.getWorld();
        long time = world.getTime();
        RangedEventHooks.tickLiving(living, world, time);
        if (living instanceof PlayerEntity player) {
            tickPlayer(player, world, time);
        }
    }

    private static void tickPlayer(PlayerEntity player, ServerWorld world, long time) {
        RangedEventHooks.tickPlayer(player, world);
        updatePhantomCharge(player, world, time);
        updateTitanCharge(player, world, time);
        updateShockCharge(player, world, time);
        updateImpactBloomState(player, time);
        updateSolarGuardState(player, time);
        updateMindwardVeil(player, world, time);
    }

    private static void updatePhantomCharge(PlayerEntity player, ServerWorld world, long time) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.PHANTOM_GUILLOTINE));
        if (level <= 0) {
            TIMESTAMPS.mark(player, "phantom_ready_until", 0);
            return;
        }
        boolean moving = player.isSprinting() || player.isFallFlying();
        if (!moving) {
            VALUES.set(player, "phantom_ticks", 0);
            return;
        }
        double ticks = VALUES.add(player, "phantom_ticks", 1, 0, 200);
        int requirement = level == 1 ? 40 : level == 2 ? 30 : 30;
        if (ticks >= requirement) {
            TIMESTAMPS.mark(player, "phantom_ready_until", time + 80);
            VALUES.set(player, "phantom_ticks", 0);
        }
    }

    private static void updateTitanCharge(PlayerEntity player, ServerWorld world, long time) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.TITAN_HEW));
        if (level <= 0) {
            TIMESTAMPS.mark(player, "titan_ready_tick", 0);
            return;
        }
        boolean holdingAxe = player.getMainHandStack().getItem() instanceof AxeItem;
        if (!holdingAxe || !player.isSneaking()) {
            VALUES.set(player, "titan_sneak", 0);
            return;
        }
        double ticks = VALUES.add(player, "titan_sneak", 1, 0, 40);
        int requirement = 20;
        if (ticks >= requirement && TIMESTAMPS.get(player, "titan_ready_tick") == 0) {
            TIMESTAMPS.mark(player, "titan_ready_tick", time);
            VALUES.set(player, "titan_sneak", 0);
        }
    }

    private static void updateShockCharge(PlayerEntity player, ServerWorld world, long time) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.SHOCK_LANCER));
        if (level <= 0) {
            TIMESTAMPS.mark(player, "shock_ready_tick", 0);
            return;
        }
        ItemStack active = player.getActiveItem();
        if (player.isUsingItem() && active.getItem() instanceof MaceItem) {
            long start = TIMESTAMPS.get(player, "shock_charge_start");
            if (start == 0) {
                TIMESTAMPS.mark(player, "shock_charge_start", time);
            } else {
                long elapsed = time - start;
                long needed = level == 1 ? 20 : 14;
                if (elapsed >= needed) {
                    TIMESTAMPS.mark(player, "shock_ready_tick", time);
                }
            }
        } else {
            TIMESTAMPS.mark(player, "shock_charge_start", 0);
            if (time - TIMESTAMPS.get(player, "shock_ready_tick") > 60) {
                TIMESTAMPS.mark(player, "shock_ready_tick", 0);
            }
        }
    }

    /**
     * Tracks fall distance so Impact Bloom only arms after meaningful drops.
     */
    private static void updateImpactBloomState(PlayerEntity player, long time) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.IMPACT_BLOOM));
        if (level <= 0) {
            TIMESTAMPS.mark(player, "impact_ready_until", 0);
            VALUES.set(player, "impact_peak_fall", 0);
            return;
        }
        double peakFall = VALUES.get(player, "impact_peak_fall");
        if (!player.isOnGround() && player.getVelocity().y < -0.08) {
            peakFall = Math.max(peakFall, player.fallDistance);
            VALUES.set(player, "impact_peak_fall", peakFall);
            return;
        }
        if (player.isOnGround()) {
            if (peakFall >= 2.0) {
                TIMESTAMPS.mark(player, "impact_ready_until", time);
            }
            VALUES.set(player, "impact_peak_fall", 0);
        }
    }

    private static void updateSolarGuardState(PlayerEntity player, long time) {
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.SOLAR_RIPOSTE), player.getMainHandStack());
        if (level <= 0) {
            TIMESTAMPS.mark(player, "solar_guard_tick", 0);
            return;
        }
        if (player.isBlocking()) {
            if (TIMESTAMPS.get(player, "solar_guard_tick") == 0) {
                TIMESTAMPS.mark(player, "solar_guard_tick", time);
            }
        } else {
            TIMESTAMPS.mark(player, "solar_guard_tick", 0);
        }
    }

    private static void updateMindwardVeil(PlayerEntity player, ServerWorld world, long time) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.MINDWARD_VEIL), helmet);
        if (level <= 0) {
            TIMESTAMPS.mark(player, "mindward_immunity_until", 0);
            return;
        }
        long immunityUntil = TIMESTAMPS.get(player, "mindward_immunity_until");
        if (immunityUntil > time) {
            cleanseMindwardStatuses(player);
        }
        if (!hasMindwardAffliction(player)) {
            return;
        }
        int cooldown = switch (level) {
            case 1 -> 200;
            case 2 -> 160;
            default -> 120;
        };
        if (COOLDOWNS.ticksRemaining(player, "mindward_veil", time) > 0) {
            return;
        }
        float chance = switch (level) {
            case 1 -> 0.25f;
            case 2 -> 0.5f;
            default -> 0.75f;
        };
        if (world.random.nextFloat() > chance) {
            return;
        }
        COOLDOWNS.ready(player, "mindward_veil", time, cooldown);
        cleanseMindwardStatuses(player);
        int resistanceDuration = switch (level) {
            case 1 -> 100;
            case 2 -> 120;
            default -> 160;
        };
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistanceDuration, 0, false, false));
        if (level >= 2) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 80, 0, false, false));
        }
        if (level >= 3) {
            TIMESTAMPS.mark(player, "mindward_immunity_until", time + 40);
        }
        ParticleUtil.burst(world, ParticleTypes.ENCHANT, player.getPos().add(0, 1.2, 0), 16 + level * 2, 0.2, 0.01);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f);
    }

    private static boolean hasMindwardAffliction(LivingEntity entity) {
        for (StatusEffect effect : MINDWARD_STATUSES) {
            if (entity.hasStatusEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    private static void cleanseMindwardStatuses(LivingEntity entity) {
        for (StatusEffect effect : MINDWARD_STATUSES) {
            if (entity.hasStatusEffect(effect)) {
                entity.removeStatusEffect(effect);
            }
        }
    }

    private static ActionResult handleBloomBinder(PlayerEntity player, ServerWorld world, BlockHitResult hit) {
        int level = EnchantmentUtil.getHeldLevel(player, ModEnchantments.get(ModEnchantmentKey.BLOOM_BINDER));
        if (level <= 0) {
            return ActionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return ActionResult.PASS;
        }
        double cap = switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            default -> 7;
        };
        if (crop.isMature(state)) {
            VALUES.add(player, "bloom_charges", 1, 0, cap);
            world.setBlockState(pos, crop.withAge(0));
            ParticleUtil.burst(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(pos), 12, 0.2, 0);
            return ActionResult.SUCCESS;
        }
        double stored = VALUES.get(player, "bloom_charges");
        if (stored > 0) {
            VALUES.add(player, "bloom_charges", -1, 0, cap);
            world.setBlockState(pos, crop.withAge(Math.min(crop.getMaxAge(), crop.getAge(state) + 2)));
            ParticleUtil.burst(world, ModParticles.VERDANT_RING, Vec3d.ofCenter(pos), 16, 0.3, 0.02);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static void handleSolarRiposteGuard(PlayerEntity player, DamageSource source, float amount) {
        World world = player.getWorld();
        if (world.isClient) {
            return;
        }
        int level = EnchantmentHelper.getLevel(ModEnchantments.get(ModEnchantmentKey.SOLAR_RIPOSTE), player.getMainHandStack());
        if (level <= 0) {
            return;
        }
        long start = TIMESTAMPS.get(player, "solar_guard_tick");
        if (start == 0) {
            return;
        }
        long window = switch (level) {
            case 1 -> 10;
            case 2 -> 13;
            default -> 16;
        };
        if (world.getTime() - start > window) {
            return;
        }
        if (!player.blockedByShield(source)) {
            return;
        }
        float reflectPercent = switch (level) {
            case 1 -> 0.2f;
            case 2 -> 0.3f;
            default -> 0.4f;
        };
        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        if (attacker != null) {
            float reflected = Math.max(0.5f, amount * reflectPercent);
            attacker.damage(world.getDamageSources().thorns(player), reflected);
            if (level >= 3) {
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0));
            }
        }
        int fireDuration = level == 1 ? 20 : level == 2 ? 25 : 40;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireDuration, 0));
        if (world instanceof ServerWorld serverWorld) {
            ParticleUtil.burst(serverWorld, ParticleTypes.FLAME, player.getPos().add(0, 1, 0), 18, 0.2, 0.01);
        }
        TIMESTAMPS.mark(player, "solar_guard_tick", 0);
    }
}
