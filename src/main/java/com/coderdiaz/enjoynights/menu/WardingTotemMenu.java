package com.coderdiaz.enjoynights.menu;

import com.coderdiaz.enjoynights.block.WardingTotemBlock;
import com.coderdiaz.enjoynights.block.entity.WardingTotemBlockEntity;
import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.init.ModBlocks;
import com.coderdiaz.enjoynights.init.ModMenuTypes;
import com.coderdiaz.enjoynights.ward.WardingSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class WardingTotemMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    @Nullable
    private final WardingTotemBlockEntity blockEntity;

    // Client-side constructor
    public WardingTotemMenu(int windowId, Inventory playerInventory, BlockPos pos) {
        this(windowId, playerInventory, pos, new SimpleContainer(1), null, new SimpleContainerData(3));
    }

    // Server-side constructor
    public WardingTotemMenu(int windowId, Inventory playerInventory, BlockPos pos, Container container, @Nullable WardingTotemBlockEntity blockEntity) {
        this(windowId, playerInventory, pos, container, blockEntity, new ContainerData() {
            @Override
            public int get(int index) {
                if (blockEntity == null) return 0;
                return switch (index) {
                    case 0 -> blockEntity.isActive() ? 1 : 0;
                    case 1 -> blockEntity.getStoredAmethysts();
                    case 2 -> EnjoyNightsConfig.SERVER.wardingTotemAmethystCost.get();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    private WardingTotemMenu(int windowId, Inventory playerInventory, BlockPos pos, Container container, @Nullable WardingTotemBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.WARDING_TOTEM.get(), windowId);
        this.container = container;
        this.data = data;
        this.blockPos = pos;
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        checkContainerSize(container, 1);
        container.startOpen(playerInventory.player);

        // Amethyst shard slot (center-left)
        this.addSlot(new Slot(container, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.AMETHYST_SHARD);
            }
        });

        // Player Inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public boolean isActive() {
        return this.data.get(0) == 1;
    }

    public int getStoredAmethysts() {
        return this.data.get(1);
    }

    public int getRequiredAmethysts() {
        int req = this.data.get(2);
        return req > 0 ? req : 8;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel) || blockEntity == null) {
            return false;
        }

        if (buttonId == 0) { // Toggle Activate / Deactivate button
            boolean currentlyActive = blockEntity.isActive();

            if (!currentlyActive) {
                // Check if player already has an active ward
                WardingSavedData savedData = WardingSavedData.get(serverLevel);
                WardingSavedData.WardEntry existing = savedData.getActiveWardForPlayer(player.getUUID());
                if (existing != null && !existing.pos().equals(blockPos)) {
                    player.displayClientMessage(Component.translatable(
                            "enjoy_nights.ward.already_active",
                            existing.pos().getX(),
                            existing.pos().getY(),
                            existing.pos().getZ()
                    ).withStyle(ChatFormatting.RED), false);
                    return false;
                }

                int required = EnjoyNightsConfig.SERVER.wardingTotemAmethystCost.get();
                ItemStack slotStack = container.getItem(0);
                if (slotStack.isEmpty() || !slotStack.is(Items.AMETHYST_SHARD) || slotStack.getCount() < required) {
                    player.displayClientMessage(Component.translatable(
                            "enjoy_nights.ward.need_amethysts",
                            required
                    ).withStyle(ChatFormatting.RED), false);
                    return false;
                }

                // Deduct amethysts and activate
                slotStack.shrink(required);
                blockEntity.setStoredAmethysts(required);
                blockEntity.setActive(true);
                blockEntity.setOwner(player.getUUID());
                blockEntity.setOwnerName(player.getScoreboardName());

                serverLevel.setBlock(blockPos, serverLevel.getBlockState(blockPos).setValue(WardingTotemBlock.ACTIVE, true), 3);
                savedData.registerWard(blockPos, player.getUUID(), player.getScoreboardName());

                serverLevel.playSound(null, blockPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable(
                        "enjoy_nights.ward.activated",
                        EnjoyNightsConfig.SERVER.wardingTotemRadius.get()
                ).withStyle(ChatFormatting.AQUA), false);
                return true;
            } else {
                // Deactivate
                boolean isOp = player instanceof net.minecraft.server.level.ServerPlayer sp &&
                        net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(sp.createCommandSourceStack());
                if (blockEntity.getOwner() != null && !blockEntity.getOwner().equals(player.getUUID()) && !isOp) {
                    player.displayClientMessage(Component.translatable(
                            "enjoy_nights.ward.not_owner",
                            blockEntity.getOwnerName()
                    ).withStyle(ChatFormatting.RED), false);
                    return false;
                }

                int refundCount = blockEntity.getStoredAmethysts();
                if (refundCount <= 0) {
                    refundCount = EnjoyNightsConfig.SERVER.wardingTotemAmethystCost.get();
                }

                blockEntity.setActive(false);
                blockEntity.setStoredAmethysts(0);
                blockEntity.setOwner(null);
                blockEntity.setOwnerName("");

                serverLevel.setBlock(blockPos, serverLevel.getBlockState(blockPos).setValue(WardingTotemBlock.ACTIVE, false), 3);
                WardingSavedData.get(serverLevel).unregisterWard(blockPos);

                ItemStack refundStack = new ItemStack(Items.AMETHYST_SHARD, refundCount);
                if (!player.getInventory().add(refundStack)) {
                    player.drop(refundStack, false);
                }

                serverLevel.playSound(null, blockPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("enjoy_nights.ward.deactivated").withStyle(ChatFormatting.YELLOW), false);
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == 0) {
                // From ward slot to player inventory
                if (!this.moveItemStackTo(slotStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to ward slot
                if (slotStack.is(Items.AMETHYST_SHARD)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 1 && index < 28) {
                    if (!this.moveItemStackTo(slotStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 28 && index < 37 && !this.moveItemStackTo(slotStack, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.WARDING_TOTEM.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
