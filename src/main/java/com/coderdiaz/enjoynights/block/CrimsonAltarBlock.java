package com.coderdiaz.enjoynights.block;

import com.coderdiaz.enjoynights.block.entity.CrimsonAltarBlockEntity;
import com.coderdiaz.enjoynights.entity.BloodMoonBossManager;
import com.coderdiaz.enjoynights.init.ModBlockEntities;
import com.coderdiaz.enjoynights.init.ModItems;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CrimsonAltarBlock extends BaseEntityBlock {
    public static final MapCodec<CrimsonAltarBlock> CODEC = simpleCodec(CrimsonAltarBlock::new);
    public static final BooleanProperty RITUAL_ACTIVE = BooleanProperty.create("ritual_active");

    public CrimsonAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RITUAL_ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RITUAL_ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrimsonAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CRIMSON_ALTAR.get(), CrimsonAltarBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(placer instanceof Player player)) {
            return;
        }

        tryActivateRitual(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(RITUAL_ACTIVE)) {
            return InteractionResult.CONSUME;
        }

        tryActivateRitual(level, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    private void tryActivateRitual(Level level, BlockPos pos, BlockState state, Player player) {
        // Validation 1: Strict surface / open sky requirement (NO CAVES)
        if (!level.canSeeSky(pos.above())) {
            player.displayClientMessage(
                    Component.translatable("enjoy_nights.altar.error.caves")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.removeBlock(pos, false);
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CRIMSON_ALTAR.get()));
            }
            return;
        }

        // Validation 2: Blood Moon & Night
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);
        boolean isNight = NightAndMoonHelper.isNight(level);
        if (!isBloodMoon || !isNight) {
            player.displayClientMessage(
                    Component.translatable("enjoy_nights.altar.error.not_blood_moon")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.removeBlock(pos, false);
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CRIMSON_ALTAR.get()));
            }
            return;
        }

        // Validation 3: Only 1 active boss at a time on server
        if (BloodMoonBossManager.isBossActive()) {
            player.displayClientMessage(
                    Component.translatable("enjoy_nights.altar.error.already_active")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            level.removeBlock(pos, false);
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CRIMSON_ALTAR.get()));
            }
            return;
        }

        // All validations pass! Start ritual
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CrimsonAltarBlockEntity altar) {
            altar.startRitual(player);
            level.setBlock(pos, state.setValue(RITUAL_ACTIVE, true), 3);
            player.displayClientMessage(
                    Component.translatable("enjoy_nights.altar.ritual_started")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    true
            );
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 1.5F);
        }
    }
}
