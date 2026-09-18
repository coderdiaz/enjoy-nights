package com.coderdiaz.enjoynights.entity.ai;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieSiegeBreakGoal extends Goal {
    private final Zombie zombie;
    private BlockPos targetBlockPos = null;
    private int breakTime = 0;
    private int lastBreakProgress = -1;
    private int totalBreakTime = 80;
    private int stuckTicks = 0;

    public ZombieSiegeBreakGoal(Zombie zombie) {
        this.zombie = zombie;
        // IMPORTANT: Must claim both MOVE and LOOK flags while breaking an obstacle.
        // This prevents MeleeAttackGoal from telling the zombie to pathfind around the building
        // while it is actively breaching a door or wall.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private LivingEntity getTargetPlayer() {
        LivingEntity target = this.zombie.getTarget();
        if (target != null && target.isAlive()) {
            if (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator())) {
                return target;
            }
        }

        // Scent of Blood: find nearest survival player within 24 blocks (even through walls and doors)
        Player nearest = this.zombie.level().getNearestPlayer(this.zombie, 24.0);
        if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && nearest.isAlive()) {
            this.zombie.setTarget(nearest);
            return nearest;
        }

        return null;
    }

    @Override
    public boolean canUse() {
        if (!EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get() ||
                !EnjoyNightsConfig.SERVER.bloodMoonZombiesBreakBlocks.get()) {
            this.stuckTicks = 0;
            return false;
        }

        Level level = this.zombie.level();
        if (!NightAndMoonHelper.isBloodMoon(level)) {
            this.stuckTicks = 0;
            return false;
        }

        LivingEntity target = getTargetPlayer();
        if (target == null || !target.isAlive()) {
            this.stuckTicks = 0;
            return false;
        }

        double distanceSqr = this.zombie.distanceToSqr(target);

        // Only search for barriers within reasonable pursuit range (1.0 to 24 blocks)
        if (distanceSqr > 576.0 || distanceSqr < 1.0) {
            this.stuckTicks = 0;
            return false;
        }

        // Verify zombie is colliding horizontally against a barrier
        if (this.zombie.horizontalCollision && this.zombie.getDeltaMovement().horizontalDistanceSqr() < 0.005) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = Math.max(0, this.stuckTicks - 2);
        }

        // Require at least 6 ticks of being pressed against an obstacle
        if (this.stuckTicks < 6) {
            return false;
        }

        // Find the blocking obstacle
        this.targetBlockPos = findActualObstacle(level, target);
        if (this.targetBlockPos != null) {
            this.totalBreakTime = calculateBreakTime(level.getBlockState(this.targetBlockPos));
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!NightAndMoonHelper.isBloodMoon(this.zombie.level())) {
            return false;
        }

        if (this.targetBlockPos == null) {
            return false;
        }

        // Block must still exist and be breakable (not already destroyed or opened)
        BlockState state = this.zombie.level().getBlockState(this.targetBlockPos);
        if (state.isAir() || !isBreakableBlock(state)) {
            return false;
        }

        // If it's a door, ensure it didn't get opened
        if (state.is(BlockTags.WOODEN_DOORS) && state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN)) {
            return false;
        }

        // Zombie must still be close to the block (within reach)
        if (!this.targetBlockPos.closerToCenterThan(this.zombie.position(), 2.8)) {
            return false;
        }

        // Target player must still be valid and alive
        LivingEntity target = getTargetPlayer();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Keep breaking until the block is fully destroyed!
        return this.breakTime <= this.totalBreakTime + 5;
    }

    @Override
    public void start() {
        this.breakTime = 0;
        this.lastBreakProgress = -1;
        // Stop any current pathfinding so the zombie stays firmly planted in front of the barrier
        this.zombie.getNavigation().stop();
    }

    @Override
    public void stop() {
        if (this.targetBlockPos != null) {
            this.zombie.level().destroyBlockProgress(this.zombie.getId(), this.targetBlockPos, -1);
            this.targetBlockPos = null;
        }
        this.breakTime = 0;
        this.lastBreakProgress = -1;
        this.stuckTicks = 0;
    }

    @Override
    public void tick() {
        if (this.targetBlockPos == null) {
            return;
        }

        Level level = this.zombie.level();

        // Keep navigation stopped during breach
        this.zombie.getNavigation().stop();

        // Look directly at the center of the block being broken
        this.zombie.getLookControl().setLookAt(
                this.targetBlockPos.getX() + 0.5,
                this.targetBlockPos.getY() + 0.5,
                this.targetBlockPos.getZ() + 0.5
        );

        BlockState state = level.getBlockState(this.targetBlockPos);

        // Visual arm swing and hit sounds
        if (this.breakTime % 15 == 0) {
            this.zombie.swing(InteractionHand.MAIN_HAND);
            if (state.is(BlockTags.WOODEN_DOORS)) {
                // Vanilla door hit event (1019 = zombie attacks door)
                level.levelEvent(1019, this.targetBlockPos, 0);
            } else {
                level.levelEvent(2001, this.targetBlockPos, Block.getId(state));
            }
        }

        this.breakTime++;

        int progress = (int) ((float) this.breakTime / (float) this.totalBreakTime * 10.0f);
        if (progress != this.lastBreakProgress) {
            level.destroyBlockProgress(this.zombie.getId(), this.targetBlockPos, progress);
            this.lastBreakProgress = progress;
        }

        // Obstacle fully breached!
        if (this.breakTime >= this.totalBreakTime) {
            level.destroyBlockProgress(this.zombie.getId(), this.targetBlockPos, -1);

            boolean isDoor = state.is(BlockTags.WOODEN_DOORS);
            BlockPos targetPos = this.targetBlockPos;

            // Destroy the block
            level.destroyBlock(targetPos, true, this.zombie, 512);

            if (isDoor) {
                // Play authentic zombie door break sound (1021 = zombie breaks door)
                level.levelEvent(1021, targetPos, 0);

                // If upper or lower half remains, destroy the matching half so doorway is fully open
                if (state.hasProperty(DoorBlock.HALF)) {
                    BlockPos otherHalf = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ?
                            targetPos.above() : targetPos.below();
                    if (level.getBlockState(otherHalf).is(BlockTags.WOODEN_DOORS)) {
                        level.destroyBlock(otherHalf, false, this.zombie, 512);
                    }
                }
            }

            this.targetBlockPos = null;
            this.stuckTicks = 0;
            this.breakTime = 0;

            // Immediately charge towards target player
            LivingEntity target = getTargetPlayer();
            if (target != null) {
                this.zombie.setTarget(target);
                this.zombie.getNavigation().moveTo(target, 1.25);
            }
        }
    }

    private BlockPos findActualObstacle(Level level, LivingEntity target) {
        BlockPos zombiePos = this.zombie.blockPosition();
        Direction facing = this.zombie.getDirection();

        // 1. Check direct facing blocks
        BlockPos frontFeet = zombiePos.relative(facing);
        BlockPos frontHead = frontFeet.above();

        BlockPos candidate = evaluateObstaclePair(level, frontFeet, frontHead, target);
        if (candidate != null) return candidate;

        // 2. Check blocks in vector direction towards target
        Vec3 diff = target.position().subtract(this.zombie.position()).normalize();
        BlockPos stepFeet = zombiePos.offset(Mth.floor(diff.x + 0.5), 0, Mth.floor(diff.z + 0.5));
        BlockPos stepHead = stepFeet.above();

        return evaluateObstaclePair(level, stepFeet, stepHead, target);
    }

    private BlockPos evaluateObstaclePair(Level level, BlockPos feetPos, BlockPos headPos, LivingEntity target) {
        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);

        // A. If feet or head is a closed wooden door/trapdoor, that's the barricade!
        if (isClosedDoorOrTrapdoor(feetState)) return feetPos;
        if (isClosedDoorOrTrapdoor(headState)) return headPos;

        // B. ANTI-MOUNTAIN CHECK:
        // If feet is a solid block, BUT head is AIR and above head is AIR,
        // this is a walkable step. Never break jumpable steps.
        if (isBreakableBlock(feetState) && headState.isAir() && level.getBlockState(headPos.above()).isAir()) {
            return null;
        }

        // C. REAL BARRICADE CHECK (Walls, bunkers):
        if (isBreakableBlock(headState) && isCloserToTarget(headPos, target)) {
            return headPos;
        }

        if (isBreakableBlock(feetState) && !headState.isAir() && isCloserToTarget(feetPos, target)) {
            return feetPos;
        }

        if (isBreakableBlock(feetState) && isCloserToTarget(feetPos, target)) {
            if (target.blockPosition().distSqr(feetPos) <= 4.0) {
                return feetPos;
            }
        }

        return null;
    }

    private boolean isClosedDoorOrTrapdoor(BlockState state) {
        if (state.is(BlockTags.WOODEN_DOORS)) {
            return !state.hasProperty(DoorBlock.OPEN) || !state.getValue(DoorBlock.OPEN);
        }
        return state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_FENCES);
    }

    private boolean isCloserToTarget(BlockPos pos, LivingEntity target) {
        return pos.distSqr(target.blockPosition()) < this.zombie.blockPosition().distSqr(target.blockPosition());
    }

    private boolean isBreakableBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        // Wood blocks: doors, trapdoors, planks, logs, fences, axe-mineable
        boolean isWood = state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.LOGS);

        if (isWood) return true;

        // Dust and dirt blocks: dirt, sand, gravel, shovel-mineable
        boolean isDustOrDirt = state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL);

        return isDustOrDirt;
    }

    private int calculateBreakTime(BlockState state) {
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)) {
            return 40; // ~2 seconds for dirt/sand
        }
        if (state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return 60; // ~3 seconds for doors
        }
        if (state.is(BlockTags.LOGS)) {
            return 100; // ~5 seconds for raw logs
        }
        return 80; // ~4 seconds for planks and other wood
    }
}
