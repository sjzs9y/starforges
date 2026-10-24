package com.starforge.whisper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 右键「加速」模式的运行时状态：记录被加速的方块与生物，持续 30 秒。
 *
 * <p>方块：每 tick 额外驱动 {@link #TICKS_PER_TICK} 次方块实体 ticker（熔炉、机器等），
 * 没有方块实体但会随机刻的方块（作物、树苗）则额外执行随机刻，从而加快生长。</p>
 *
 * <p>生物：只推进计时器（幼年成长、繁殖冷却、下蛋、长毛、蜜蜂状态），
 * 不跑 AI / 寻路，避免生物抽风。</p>
 *
 * <p>状态只存在内存里，重进世界后会清空。</p>
 */
public final class WhisperAccelerator {

    /** 加速持续 30 秒。 */
    public static final int DURATION_TICKS = 600;

    /** 每游戏 tick 额外推进的次数（10 ≈ 11 倍速）。 */
    public static final int TICKS_PER_TICK = 10;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> BLOCKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, Long>> ENTITIES = new HashMap<>();

    public static void accelerateBlock(ServerLevel level, BlockPos pos) {
        BLOCKS.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(pos.immutable(), level.getGameTime() + DURATION_TICKS);
    }

    public static void accelerateEntity(ServerLevel level, LivingEntity entity) {
        ENTITIES.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(entity.getUUID(), level.getGameTime() + DURATION_TICKS);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();

        Map<BlockPos, Long> blocks = BLOCKS.get(level.dimension());
        if (blocks != null && !blocks.isEmpty()) {
            blocks.entrySet().removeIf(entry -> entry.getValue() < now);
            for (BlockPos pos : blocks.keySet().toArray(new BlockPos[0])) {
                if (!tickBlock(level, pos)) {
                    blocks.remove(pos);
                }
            }
        }

        Map<UUID, Long> entities = ENTITIES.get(level.dimension());
        if (entities != null && !entities.isEmpty()) {
            entities.entrySet().removeIf(entry -> entry.getValue() < now);
            for (UUID id : entities.keySet().toArray(new UUID[0])) {
                if (!(level.getEntity(id) instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                    entities.remove(id);
                    continue;
                }
                tickEntityTimers(living);
            }
        }
    }

    /**
     * 推进一个方块。返回 {@code false} 表示这个位置已经不值得继续加速（被挖掉 / 没有可 tick 的东西）。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean tickBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            BlockEntityTicker ticker = state.getTicker(level, blockEntity.getType());
            if (ticker == null) {
                return false;
            }
            for (int i = 0; i < TICKS_PER_TICK; i++) {
                ticker.tick(level, pos, state, blockEntity);
            }
            return true;
        }

        if (!state.isRandomlyTicking()) {
            return false;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < TICKS_PER_TICK; i++) {
            state.randomTick(level, pos, random);
        }
        return true;
    }

    /** 只推进生物身上的计时器，不跑 AI。 */
    private static void tickEntityTimers(LivingEntity entity) {
        int extra = TICKS_PER_TICK;

        // 幼年成长（age < 0 向 0 推进）+ 繁殖后冷却（age > 0 向 0 递减）
        if (entity instanceof AgeableMob ageable) {
            int age = ageable.getAge();
            if (age != 0) {
                int step = Math.min(Math.abs(age), extra);
                ageable.setAge(age < 0 ? age + step : age - step);
            }
        }

        // 鸡下蛋
        if (entity instanceof Chicken chicken && !chicken.isBaby()) {
            chicken.eggTime = Math.max(0, chicken.eggTime - extra);
        }

        // 羊剪毛后长毛
        if (entity instanceof Sheep sheep && !sheep.isBaby() && sheep.isSheared()) {
            sheep.setSheared(false);
        }

        // 蜜蜂：不再疲倦、允许回巢
        if (entity instanceof Bee bee) {
            bee.resetTicksWithoutNectarSinceExitingHive();
            bee.setStayOutOfHiveCountdown(0);
        }
    }

    private WhisperAccelerator() {
    }
}
