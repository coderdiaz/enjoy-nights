package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class LunarPhaseEvents {
    public static final String TAG_POWERED_MOB = "enjoy_nights:powered";
    public static final String TAG_EXTRA_SPAWN = "enjoy_nights:extra_spawn";

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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        // New Moon lower visibility when outside under the dark night sky
        if (player.tickCount % 40 == 0 && NightAndMoonHelper.isNewMoon(level)) {
            if (level.canSeeSky(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false, false));
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

            if (random.nextFloat() < buffChance) {
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
                                Mob companion = (Mob) monster.getType().create(serverLevel, EntitySpawnReason.EVENT);
                                if (companion != null) {
                                    companion.setPos(companionPos.getX() + 0.5, (double) companionPos.getY(), companionPos.getZ() + 0.5);
                                    companion.setYRot(random.nextFloat() * 360f);
                                    companion.addTag(TAG_EXTRA_SPAWN);
                                    companion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(companionPos), EntitySpawnReason.EVENT, null);
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
}
