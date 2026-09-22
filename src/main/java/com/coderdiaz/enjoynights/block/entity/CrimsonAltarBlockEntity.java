package com.coderdiaz.enjoynights.block.entity;

import com.coderdiaz.enjoynights.entity.BloodMoonBossManager;
import com.coderdiaz.enjoynights.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class CrimsonAltarBlockEntity extends BlockEntity {

    private boolean ritualActive = false;
    private int ritualTicks = 0;
    private UUID summonerUuid = null;

    public CrimsonAltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRIMSON_ALTAR.get(), pos, blockState);
    }

    public void startRitual(Player summoner) {
        this.ritualActive = true;
        this.ritualTicks = 0;
        this.summonerUuid = summoner.getUUID();
        setChanged();
    }

    public boolean isRitualActive() {
        return ritualActive;
    }

    public UUID getSummonerUuid() {
        return summonerUuid;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrimsonAltarBlockEntity altar) {
        if (!altar.ritualActive) {
            return;
        }

        altar.ritualTicks++;

        if (level instanceof ServerLevel serverLevel) {
            // Ascending particles of soul fire and arcane runes
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    4,
                    0.25,
                    0.4,
                    0.25,
                    0.03
            );
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5,
                    pos.getY() + 1.2,
                    pos.getZ() + 0.5,
                    5,
                    0.3,
                    0.5,
                    0.3,
                    0.5
            );

            // Escalating ritual audio every second
            if (altar.ritualTicks % 20 == 0) {
                float pitch = 0.8F + (altar.ritualTicks / 80.0F) * 0.4F;
                serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.3F, pitch);
                serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.4F, 0.75F);
            }

            // At 80 ticks (4 seconds), strike lightning, destroy altar, and spawn the boss!
            if (altar.ritualTicks >= 80) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
                if (bolt != null) {
                    bolt.setPos(pos.getX() + 0.5, (double) pos.getY(), pos.getZ() + 0.5);
                    bolt.setVisualOnly(true);
                    serverLevel.addFreshEntity(bolt);
                }

                serverLevel.removeBlock(pos, false);

                Player summoner = altar.summonerUuid != null ? serverLevel.getPlayerByUUID(altar.summonerUuid) : null;
                BloodMoonBossManager.spawnBossFromRitual(serverLevel, pos, summoner);
                altar.ritualActive = false;
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("ritualActive", this.ritualActive);
        out.putInt("ritualTicks", this.ritualTicks);
        if (this.summonerUuid != null) {
            out.putString("summonerUuid", this.summonerUuid.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.ritualActive = in.getBooleanOr("ritualActive", false);
        this.ritualTicks = in.getIntOr("ritualTicks", 0);
        String uuidStr = in.getStringOr("summonerUuid", "");
        if (!uuidStr.isEmpty()) {
            try {
                this.summonerUuid = UUID.fromString(uuidStr);
            } catch (Exception ignored) {
                this.summonerUuid = null;
            }
        } else {
            this.summonerUuid = null;
        }
    }
}
