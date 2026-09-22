package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;

public class LunarPhaseEvents {
    public static final String TAG_POWERED_MOB = "enjoy_nights:powered";
    public static final String TAG_EXTRA_SPAWN = "enjoy_nights:extra_spawn";
    public static final String TAG_FIRE_SKELETON = "enjoy_nights:fire_skeleton";

    @SubscribeEvent
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player) {
            Level level = player.level();
            if (NightAndMoonHelper.isNewMoon(level)) {
                // In New Moon, stealth is significantly increased for the player
                double stealthFactor = EnjoyNightsConfig.SERVER.newMoonStealthFactor.get();
                event.modifyVisibility(stealthFactor);
            }
        }
    }


    @SubscribeEvent
    public static void onMonsterSpawn(FinalizeSpawnEvent event) {
        if (!EnjoyNightsConfig.SERVER.fullMoonMobBuffsEnabled.get()) {
            return;
        }

        Level level = event.getLevel().getLevel();
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }

        boolean isFullMoon = NightAndMoonHelper.isFullMoon(level);
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);

        if (isFullMoon || isBloodMoon) {
            RandomSource random = monster.getRandom();
            // Higher chance to spawn with buffs (75% on Blood Moon, 50% on Full Moon)
            float buffChance = isBloodMoon ? 0.75f : 0.50f;

            if (monster instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                // Do NOT give potion effects to creepers to avoid lingering AreaEffectCloud exploit on explosion
                double chargedChance = isBloodMoon ?
                        EnjoyNightsConfig.SERVER.bloodMoonChargedCreeperChance.get() :
                        EnjoyNightsConfig.SERVER.fullMoonChargedCreeperChance.get();

                if (random.nextFloat() < chargedChance) {
                    com.coderdiaz.enjoynights.util.CreeperHelper.setPowered(creeper, true);
                }

                // Agility speed increase directly through attributes (no potion clouds)
                AttributeInstance speedAttr = creeper.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.setBaseValue(speedAttr.getBaseValue() * 1.15);
                }

                creeper.addTag(TAG_POWERED_MOB);
            } else if (random.nextFloat() < buffChance) {
                // Add Strength
                monster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, isBloodMoon ? 1 : 0, false, true, true));

                // 50% chance for Speed
                if (random.nextFloat() < 0.50f) {
                    monster.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));
                }

                // If Blood Moon, also grant Resistance
                if (isBloodMoon) {
                    monster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 0, false, true, true));
                }

                // Increase follow range to make mobs more aggressive
                AttributeInstance followRange = monster.getAttribute(Attributes.FOLLOW_RANGE);
                if (followRange != null) {
                    followRange.setBaseValue(followRange.getBaseValue() + 16.0);
                }

                monster.addTag(TAG_POWERED_MOB);
            }

            // Flame Skeletons in Blood Moon (15% chance to shoot flaming arrows)
            if (isBloodMoon && monster instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton skeleton) {
                double flameChance = EnjoyNightsConfig.SERVER.bloodMoonFlameSkeletonChance.get();
                if (random.nextFloat() < flameChance) {
                    skeleton.addTag(TAG_FIRE_SKELETON);
                    skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHAINMAIL_HELMET));
                    skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0F);
                    net.minecraft.world.item.ItemStack bow = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW);
                    net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> flameHolder =
                            level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FLAME);
                    bow.enchant(flameHolder, 1);
                    skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, bow);
                    skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
                }
            }

            // Controlled extra natural mob spawn during Blood Moon
            if (isBloodMoon && EnjoyNightsConfig.SERVER.bloodMoonExtraSpawnsEnabled.get()) {
                if (event.getSpawnType() == EntitySpawnReason.NATURAL && !monster.getTags().contains(TAG_EXTRA_SPAWN)) {
                    double extraChance = EnjoyNightsConfig.SERVER.bloodMoonExtraNaturalSpawnChance.get();
                    if (random.nextFloat() < extraChance && event.getLevel() instanceof ServerLevel serverLevel) {
                        AABB box = monster.getBoundingBox().inflate(16.0);
                        // Prevent excessive mobs in the immediate area
                        if (serverLevel.getEntitiesOfClass(Monster.class, box).size() < 10) {
                            BlockPos companionPos = monster.blockPosition().offset(
                                    random.nextInt(5) - 2,
                                    0,
                                    random.nextInt(5) - 2
                            );
                            if (serverLevel.getBlockState(companionPos).isAir()) {
                                Mob companion = (Mob) monster.getType().create(serverLevel, EntitySpawnReason.NATURAL);
                                if (companion != null) {
                                    companion.setPos(companionPos.getX() + 0.5, (double) companionPos.getY(), companionPos.getZ() + 0.5);
                                    companion.setYRot(random.nextFloat() * 360f);
                                    companion.addTag(TAG_EXTRA_SPAWN);
                                    companion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(companionPos), EntitySpawnReason.NATURAL, null);
                                    serverLevel.addFreshEntity(companion);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }

        Level level = monster.level();
        boolean isPowered = monster.getTags().contains(TAG_POWERED_MOB);
        boolean isFullMoon = NightAndMoonHelper.isFullMoon(level);
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);

        if (isPowered || isFullMoon || isBloodMoon) {
            int multiplier = EnjoyNightsConfig.SERVER.fullMoonExperienceMultiplier.get();
            event.setDroppedExperience(event.getDroppedExperience() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onArrowSpawn(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
            if (arrow.getOwner() instanceof net.minecraft.world.entity.LivingEntity shooter) {
                if (shooter.getTags().contains(TAG_FIRE_SKELETON)) {
                    arrow.igniteForSeconds(100);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(TAG_FIRE_SKELETON)) {
            // Guarantee that flame skeletons never drop any bow or chainmail helmet
            event.getDrops().removeIf(drop -> drop.getItem().is(net.minecraft.world.item.Items.BOW)
                    || drop.getItem().is(net.minecraft.world.item.Items.CHAINMAIL_HELMET));
        }
    }

    @SubscribeEvent
    public static void onMobDespawn(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (mob.level().isClientSide() || !(mob.level() instanceof ServerLevel level)) {
            return;
        }

        boolean isExtraSpawn = mob.getTags().contains(TAG_EXTRA_SPAWN);
        boolean isFireSkeleton = mob.getTags().contains(TAG_FIRE_SKELETON);

        if (isExtraSpawn || isFireSkeleton) {
            // If daytime has arrived, immediately force despawn of these night mobs
            if (!NightAndMoonHelper.isNight(level)) {
                event.setResult(MobDespawnEvent.Result.ALLOW);
                return;
            }

            // Outside Blood Moon, force despawn extra siege/companion mobs if distant from players
            if (!NightAndMoonHelper.isBloodMoon(level) && isExtraSpawn) {
                Player nearest = level.getNearestPlayer(mob, 40.0);
                if (nearest == null) {
                    event.setResult(MobDespawnEvent.Result.ALLOW);
                }
            }
        }
    }
}
