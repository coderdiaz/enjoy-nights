package com.coderdiaz.enjoynights.entity.ai;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;

/**
 * During Blood Moon, zombies sense players through walls and doors ("Blood Scent"),
 * ensuring they do not lose target when a player enters a house or structure.
 */
public class BloodMoonSiegeTargetGoal extends NearestAttackableTargetGoal<Player> {
    private final Zombie zombie;

    public BloodMoonSiegeTargetGoal(Zombie zombie) {
        // mustSee = false, mustReach = false
        super(zombie, Player.class, 10, false, false, null);
        this.zombie = zombie;
        // Explicitly configure targeting conditions to ignore line of sight during Blood Moon
        this.targetConditions = TargetingConditions.forCombat()
                .range(32.0)
                .ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        if (!EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get()) {
            return false;
        }

        if (!NightAndMoonHelper.isBloodMoon(this.zombie.level())) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!NightAndMoonHelper.isBloodMoon(this.zombie.level())) {
            return false;
        }

        return super.canContinueToUse();
    }
}
