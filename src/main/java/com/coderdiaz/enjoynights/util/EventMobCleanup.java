package com.coderdiaz.enjoynights.util;

import com.coderdiaz.enjoynights.entity.BloodMoonBossManager;
import com.coderdiaz.enjoynights.event.LunarPhaseEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Centralizes the lifecycle cleanup of mobs created or empowered by lunar events
 * (Full Moon / Blood Moon). A single implementation is shared by the end-of-event
 * sweep and the chunk-load handler so both behave identically.
 *
 * <p>Event-spawned mobs ({@code extra_spawn}, {@code fire_skeleton}, boss) are
 * deleted, because they only exist as a consequence of the event. Naturally-spawned
 * mobs that were merely empowered ({@code powered}) are normalized instead of
 * deleted, preserving legitimate world population.
 *
 * <p>Empowerment is applied via named, removable {@link AttributeModifier}s with the
 * IDs below, so reversal is exact and idempotent: removing a modifier that is not
 * present is a no-op, and a mob can never be "double-stripped" into a corrupted state.
 */
public final class EventMobCleanup {
    /** Follow-range bonus granted to empowered non-creeper mobs (+16 blocks). */
    public static final Identifier MODIFIER_FOLLOW_RANGE =
            Identifier.fromNamespaceAndPath("enjoy_nights", "lunar_follow_range");
    /** Movement-speed bonus granted to empowered creepers (+15%). */
    public static final Identifier MODIFIER_CREEPER_SPEED =
            Identifier.fromNamespaceAndPath("enjoy_nights", "lunar_creeper_speed");

    /** The outcome of evaluating a single tagged mob, for diagnostics/counting. */
    public enum Action {
        NONE,
        DELETED,
        UNBUFFED
    }

    private EventMobCleanup() {
    }

    /**
     * Evaluates a single mob and applies the correct lifecycle action.
     *
     * @param mob        the mob to evaluate
     * @param bossActive whether a Blood Moon boss battle is currently in progress
     *                   (an active boss is owned by {@link BloodMoonBossManager} and
     *                   must not be deleted here)
     * @param forceDelete when true, empowered mobs are deleted outright instead of
     *                    being un-buffed (used by the admin purge command)
     * @return the action that was taken
     */
    public static Action cleanupTaggedMob(Mob mob, boolean bossActive, boolean forceDelete) {
        if (mob == null || !mob.isAlive()) {
            return Action.NONE;
        }

        boolean isBoss = mob.getTags().contains(BloodMoonBossManager.TAG_BOSS);
        boolean isExtraSpawn = mob.getTags().contains(LunarPhaseEvents.TAG_EXTRA_SPAWN);
        boolean isFireSkeleton = mob.getTags().contains(LunarPhaseEvents.TAG_FIRE_SKELETON);
        boolean isPowered = mob.getTags().contains(LunarPhaseEvents.TAG_POWERED_MOB);

        // Event-spawned mobs only exist because of the event: always delete.
        if (isExtraSpawn || isFireSkeleton) {
            // A live boss keeps its summoned minions until it is resolved elsewhere.
            if (isBoss && bossActive) {
                return Action.NONE;
            }
            mob.discard();
            return Action.DELETED;
        }

        if (isBoss) {
            // Boss lifecycle is managed by BloodMoonBossManager; never touch an active boss.
            if (bossActive) {
                return Action.NONE;
            }
            mob.discard();
            return Action.DELETED;
        }

        // Naturally-spawned, merely-empowered mob.
        if (isPowered) {
            if (forceDelete) {
                mob.discard();
                return Action.DELETED;
            }
            stripEventBuffs(mob);
            return Action.UNBUFFED;
        }

        return Action.NONE;
    }

    /**
     * Removes every empowerment applied by the lunar events and clears the
     * {@code powered} tag so the mob reverts to an ordinary monster. Idempotent:
     * safe to call on a mob that has already been normalized.
     */
    public static void stripEventBuffs(Mob mob) {
        // Infinite potion effects granted on spawn.
        mob.removeEffect(MobEffects.STRENGTH);
        mob.removeEffect(MobEffects.SPEED);
        mob.removeEffect(MobEffects.RESISTANCE);

        // Removable attribute modifiers (exact reversal regardless of call count).
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && followRange.getModifier(MODIFIER_FOLLOW_RANGE) != null) {
            followRange.removeModifier(MODIFIER_FOLLOW_RANGE);
        }

        if (mob instanceof Creeper creeper) {
            AttributeInstance speed = creeper.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(MODIFIER_CREEPER_SPEED) != null) {
                speed.removeModifier(MODIFIER_CREEPER_SPEED);
            }
            // Revert charged-creeper state applied during empowerment.
            CreeperHelper.setPowered(creeper, false);
        }

        mob.removeTag(LunarPhaseEvents.TAG_POWERED_MOB);
    }

    /**
     * Builds the follow-range modifier (+16 blocks, additive).
     */
    public static AttributeModifier followRangeModifier() {
        return new AttributeModifier(MODIFIER_FOLLOW_RANGE, 16.0, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * Builds the creeper movement-speed modifier (+15%, multiplicative).
     */
    public static AttributeModifier creeperSpeedModifier() {
        return new AttributeModifier(MODIFIER_CREEPER_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
