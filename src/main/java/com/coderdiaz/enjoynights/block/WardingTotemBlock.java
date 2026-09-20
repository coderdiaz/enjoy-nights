package com.coderdiaz.enjoynights.block;

import com.coderdiaz.enjoynights.block.entity.WardingTotemBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class WardingTotemBlock extends BaseEntityBlock {
    public static final MapCodec<WardingTotemBlock> CODEC = simpleCodec(WardingTotemBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public WardingTotemBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardingTotemBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Permanent Destruction Rule:
     * The Warding Totem block is NEVER dropped upon breaking (even with Silk Touch pickaxe).
     * It is completely destroyed.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof WardingTotemBlockEntity totem) {
            // Check ownership
            boolean isOp = player instanceof net.minecraft.server.level.ServerPlayer sp &&
                    net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(sp.createCommandSourceStack());
            if (totem.isActive() && totem.getOwner() != null && !totem.getOwner().equals(player.getUUID()) && !isOp) {
                player.displayClientMessage(Component.translatable(
                        "enjoy_nights.ward.not_owner",
                        totem.getOwnerName()
                ).withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }

            player.openMenu(totem, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;

            // Mystic soul flame floating upwards
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.02, 0.0);

            // Occasional enchantment glyphs floating towards the totem
            if (random.nextFloat() < 0.35f) {
                double gx = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
                double gy = pos.getY() + 0.8 + random.nextDouble();
                double gz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
                level.addParticle(ParticleTypes.ENCHANT, gx, gy, gz, (pos.getX() + 0.5) - gx, (pos.getY() + 0.8) - gy, (pos.getZ() + 0.5) - gz);
            }
        }
    }
}
