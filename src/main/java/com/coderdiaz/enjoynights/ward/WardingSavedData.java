package com.coderdiaz.enjoynights.ward;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WardingSavedData extends SavedData {
    public static final String DATA_NAME = "enjoy_nights_wards";

    public record WardEntry(BlockPos pos, UUID owner, String ownerName, long timestamp) {}

    private final Map<BlockPos, WardEntry> wards = new ConcurrentHashMap<>();

    public static final Codec<WardingSavedData> CODEC = CompoundTag.CODEC.xmap(
            WardingSavedData::load,
            data -> data.save(new CompoundTag())
    );

    public static final SavedDataType<WardingSavedData> TYPE = new SavedDataType<>(
            DATA_NAME,
            WardingSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public WardingSavedData() {
    }

    public static WardingSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<WardEntry> getAllWards() {
        return wards.values();
    }

    @Nullable
    public WardEntry getWard(BlockPos pos) {
        return wards.get(pos);
    }

    @Nullable
    public WardEntry getActiveWardForPlayer(UUID playerUuid) {
        if (playerUuid == null) return null;
        for (WardEntry entry : wards.values()) {
            if (playerUuid.equals(entry.owner())) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public WardEntry findNearestActiveWard(BlockPos pos, double maxRadius) {
        double maxRadiusSq = maxRadius * maxRadius;
        WardEntry closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (WardEntry entry : wards.values()) {
            double distSq = entry.pos().distSqr(pos);
            if (distSq <= maxRadiusSq && distSq < closestDistSq) {
                closest = entry;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    /**
     * Registers a new active ward.
     * Enforces the rule: A player can only have ONE active ward on the server.
     * If the player already has an active ward at another location, registration fails.
     */
    public boolean registerWard(BlockPos pos, UUID owner, String ownerName) {
        WardEntry existing = getActiveWardForPlayer(owner);
        if (existing != null && !existing.pos().equals(pos)) {
            return false;
        }

        wards.put(pos, new WardEntry(pos, owner, ownerName, System.currentTimeMillis()));
        setDirty();
        return true;
    }

    /**
     * Unregisters an active ward.
     */
    @Nullable
    public WardEntry unregisterWard(BlockPos pos) {
        WardEntry removed = wards.remove(pos);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    /**
     * Repels/relocates a mob spawn outside the protection perimeter (distance wardRadius + 2 to 6 blocks).
     * Calculates the surface height at the projected perimeter so the mob lands safely on the ground.
     */
    public static BlockPos calculatePerimeterRelocation(ServerLevel level, BlockPos mobPos, BlockPos wardPos, double wardRadius) {
        double dx = mobPos.getX() - wardPos.getX();
        double dz = mobPos.getZ() - wardPos.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        double angle;
        if (dist < 0.5) {
            angle = level.getRandom().nextDouble() * (2.0 * Math.PI);
        } else {
            angle = Math.atan2(dz, dx);
        }

        // Project outside the ward radius by 2 to 6 blocks
        double targetDist = wardRadius + 2.0 + level.getRandom().nextDouble() * 4.0;
        int targetX = Mth.floor(wardPos.getX() + Math.cos(angle) * targetDist);
        int targetZ = Mth.floor(wardPos.getZ() + Math.sin(angle) * targetDist);

        int targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);

        // Sanity check bounds
        if (targetY < level.getMinY() || targetY >= level.getMaxY()) {
            targetY = mobPos.getY();
        }

        return new BlockPos(targetX, targetY, targetZ);
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (WardEntry entry : wards.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.pos().asLong());
            entryTag.putString("owner", entry.owner().toString());
            entryTag.putString("ownerName", entry.ownerName());
            entryTag.putLong("timestamp", entry.timestamp());
            list.add(entryTag);
        }
        tag.put("wards", list);
        return tag;
    }

    public static WardingSavedData load(CompoundTag tag) {
        WardingSavedData data = new WardingSavedData();
        ListTag list = tag.getListOrEmpty("wards");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompoundOrEmpty(i);
            long posLong = entryTag.getLongOr("pos", 0L);
            String ownerStr = entryTag.getStringOr("owner", "");
            String ownerName = entryTag.getStringOr("ownerName", "");
            long timestamp = entryTag.getLongOr("timestamp", 0L);
            if (!ownerStr.isEmpty()) {
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    BlockPos pos = BlockPos.of(posLong);
                    data.wards.put(pos, new WardEntry(pos, owner, ownerName, timestamp));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return data;
    }
}
