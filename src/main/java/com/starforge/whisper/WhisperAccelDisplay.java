package com.starforge.whisper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 「加速」的悬浮文字载体。
 *
 * <p>它本身不做任何逻辑，只负责把「倍率」和「剩余 tick」通过 {@link SynchedEntityData} 同步给客户端，
 * 由 {@code WhisperAccelDisplayRenderer} 画成两行朝向玩家的文字。</p>
 *
 * <p>加速的实际推进仍然由 {@link WhisperAccelerator} 负责，这样方块和生物可以共用同一套计时逻辑。</p>
 */
public class WhisperAccelDisplay extends Entity {

    private static final EntityDataAccessor<Integer> DATA_MULTIPLIER =
            SynchedEntityData.defineId(WhisperAccelDisplay.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING =
            SynchedEntityData.defineId(WhisperAccelDisplay.class, EntityDataSerializers.INT);

    public WhisperAccelDisplay(EntityType<? extends WhisperAccelDisplay> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public WhisperAccelDisplay(Level level) {
        this(com.starforge.registry.ModEntities.ACCEL_DISPLAY.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MULTIPLIER, 1);
        builder.define(DATA_REMAINING, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 纯显示用实体，不需要存档
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 纯显示用实体，不需要存档
    }

    /** 纯显示实体不写进区块，重进存档时不会再出现幽灵图标。 */
    @Override
    public boolean save(CompoundTag tag) {
        return false;
    }

    /** 加速器里已经没有对应目标时自毁，用于清理旧存档残留的显示实体。 */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !WhisperAccelerator.isTracked(this)) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0D * 64.0D;
    }

    public int getMultiplier() {
        return this.entityData.get(DATA_MULTIPLIER);
    }

    public int getRemainingTicks() {
        return this.entityData.get(DATA_REMAINING);
    }

    public void setDisplay(int multiplier, int remainingTicks) {
        this.entityData.set(DATA_MULTIPLIER, multiplier);
        this.entityData.set(DATA_REMAINING, remainingTicks);
    }
}
