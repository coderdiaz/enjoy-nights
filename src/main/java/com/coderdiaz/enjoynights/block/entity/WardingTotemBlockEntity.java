package com.coderdiaz.enjoynights.block.entity;

import com.coderdiaz.enjoynights.init.ModBlockEntities;
import com.coderdiaz.enjoynights.menu.WardingTotemMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.UUID;

public class WardingTotemBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            WardingTotemBlockEntity.this.setChanged();
        }
    };

    private boolean active = false;
    private int storedAmethysts = 0;
    @Nullable
    private UUID owner = null;
    private String ownerName = "";

    public WardingTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARDING_TOTEM.get(), pos, state);
    }

    public Container getContainer() {
        return container;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setChanged();
    }

    public int getStoredAmethysts() {
        return storedAmethysts;
    }

    public void setStoredAmethysts(int storedAmethysts) {
        this.storedAmethysts = storedAmethysts;
        setChanged();
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enjoy_nights.warding_totem");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new WardingTotemMenu(windowId, playerInventory, this.worldPosition, this.container, this);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("active", this.active);
        out.putInt("storedAmethysts", this.storedAmethysts);
        if (this.owner != null) {
            out.putString("owner", this.owner.toString());
        }
        out.putString("ownerName", this.ownerName);

        ItemStack slotStack = this.container.getItem(0);
        if (!slotStack.isEmpty()) {
            out.store("slotItem", ItemStack.CODEC, slotStack);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.active = in.getBooleanOr("active", false);
        this.storedAmethysts = in.getIntOr("storedAmethysts", 0);
        String ownerStr = in.getStringOr("owner", "");
        if (!ownerStr.isEmpty()) {
            try {
                this.owner = UUID.fromString(ownerStr);
            } catch (Exception ignored) {
                this.owner = null;
            }
        } else {
            this.owner = null;
        }
        this.ownerName = in.getStringOr("ownerName", "");

        in.read("slotItem", ItemStack.CODEC).ifPresent(s -> this.container.setItem(0, s));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.coderdiaz.enjoynights.ward.WardingSavedData.get(serverLevel).unregisterWard(pos);
            if (this.storedAmethysts > 0) {
                net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, this.storedAmethysts));
            }
            ItemStack slotItem = this.container.getItem(0);
            if (!slotItem.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), slotItem);
            }
        }
    }
}
