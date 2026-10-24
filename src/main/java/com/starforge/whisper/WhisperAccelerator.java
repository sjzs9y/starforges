package com.starforge.whisper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 右键「加速」的运行时状态。
 *
 * <p>第一次右键挂 1 倍，之后每次翻倍（上限 {@link #MAX_MULTIPLIER}），每次刷新回 30 秒。
 * 方块按倍率驱动方块实体 ticker / 随机刻；生物只推进计时器。</p>
 *
 * <p>显示由 {@link WhisperAccelDisplay} 实体负责，渲染交给客户端，
 * 加速逻辑本身与显示解耦，所以显示最多 32 个也不影响实际加速。</p>
 *
 * <p>状态只存在内存里，重进世界会清空。</p>
 */
public final class WhisperAccelerator {

    /** 加速持续 30 秒。 */
    public static final int DURATION_TICKS = 600;

    /** 倍率上限。 */
    public static final int MAX_MULTIPLIER = 64;

    /** 同时最多显示多少个悬浮文字，防止极端情况实体爆炸。 */
    public static final int MAX_DISPLAYS = 32;

    /** 每秒刷新一次文字。 */
    private static final int DISPLAY_INTERVAL = 20;

    /** 生物移动超过这个距离才更新显示坐标，避免每 tick 刷包。 */
    private static final double MOVE_THRESHOLD_SQR = 0.05D * 0.05D;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Target>> BLOCKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, Target>> ENTITIES = new HashMap<>();

    private static final class Target {
        int multiplier = 1;
        long expireTick;
        WhisperAccelDisplay display;
    }

    public static void accelerateBlock(ServerLevel level, BlockPos pos) {
        BlockPos key = pos.immutable();
        Map<BlockPos, Target> map = BLOCKS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Target target = map.get(key);
        if (target == null) {
            target = new Target();
            map.put(key, target);
            target.display = spawnDisplay(level, Vec3.atCenterOf(key).add(0.0D, 1.3D, 0.0D), displayCount(level));
        } else {
            target.multiplier = Math.min(MAX_MULTIPLIER, target.multiplier * 2);
        }
        target.expireTick = level.getGameTime() + DURATION_TICKS;
        updateDisplay(level, target);
    }

    public static void accelerateEntity(ServerLevel level, LivingEntity entity) {
        UUID id = entity.getUUID();
        Map<UUID, Target> map = ENTITIES.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Target target = map.get(id);
        if (target == null) {
            target = new Target();
            map.put(id, target);
            target.display = spawnDisplay(level, displayPosFor(entity), displayCount(level));
        } else {
            target.multiplier = Math.min(MAX_MULTIPLIER, target.multiplier * 2);
        }
        target.expireTick = level.getGameTime() + DURATION_TICKS;
        updateDisplay(level, target);
    }

    /** 这个位置是否值得加速（有 ticker 的机器，或会随机刻的作物）。 */
    public static boolean isAccelerable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return state.getTicker(level, blockEntity.getType()) != null;
        }
        return state.isRandomlyTicking();
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();

        Map<BlockPos, Target> blocks = BLOCKS.get(level.dimension());
        if (blocks != null && !blocks.isEmpty()) {
            for (BlockPos pos : blocks.keySet().toArray(new BlockPos[0])) {
                Target target = blocks.get(pos);
                if (target == null) {
                    continue;
                }
                if (target.expireTick < now || !tickBlock(level, pos, target.multiplier)) {
                    discardDisplay(target);
                    blocks.remove(pos);
                    continue;
                }
                if (now % DISPLAY_INTERVAL == 0) {
                    updateDisplay(level, target);
                }
            }
        }

        Map<UUID, Target> entities = ENTITIES.get(level.dimension());
        if (entities != null && !entities.isEmpty()) {
            for (UUID id : entities.keySet().toArray(new UUID[0])) {
                Target target = entities.get(id);
                if (target == null) {
                    continue;
                }
                if (!(level.getEntity(id) instanceof LivingEntity living)
                        || !living.isAlive() || living.isRemoved()
                        || target.expireTick < now) {
                    discardDisplay(target);
                    entities.remove(id);
                    continue;
                }
                for (int i = 0; i < target.multiplier; i++) {
                    tickEntityTimers(living);
                }
                moveDisplay(living, target);
                if (now % DISPLAY_INTERVAL == 0) {
                    updateDisplay(level, target);
                }
            }
        }
    }

    /** 世界加载时清掉上一次残留的显示实体（加速状态不持久，留着只会变成幽灵文字）。 */
    public static void clearOrphanDisplays(ServerLevel level) {
        List<Entity> orphans = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof WhisperAccelDisplay) {
                orphans.add(entity);
            }
        }
        for (Entity orphan : orphans) {
            orphan.discard();
        }
    }

    // ------------------------------------------------------------------ 显示

    private static int displayCount(ServerLevel level) {
        int count = 0;
        Map<BlockPos, Target> blocks = BLOCKS.get(level.dimension());
        if (blocks != null) {
            count += blocks.size();
        }
        Map<UUID, Target> entities = ENTITIES.get(level.dimension());
        if (entities != null) {
            count += entities.size();
        }
        return count;
    }

    private static Vec3 displayPosFor(LivingEntity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.8D, entity.getZ());
    }

    private static WhisperAccelDisplay spawnDisplay(ServerLevel level, Vec3 pos, int currentCount) {
        if (currentCount >= MAX_DISPLAYS) {
            return null;
        }
        WhisperAccelDisplay display = new WhisperAccelDisplay(level);
        display.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        display.setNoGravity(true);
        level.addFreshEntity(display);
        return display;
    }

    private static void moveDisplay(LivingEntity living, Target target) {
        if (target.display == null || target.display.isRemoved()) {
            return;
        }
        Vec3 wanted = displayPosFor(living);
        if (target.display.position().distanceToSqr(wanted) > MOVE_THRESHOLD_SQR) {
            target.display.moveTo(wanted.x, wanted.y, wanted.z, 0.0F, 0.0F);
        }
    }

    private static void updateDisplay(ServerLevel level, Target target) {
        if (target.display == null || target.display.isRemoved()) {
            return;
        }
        long remainTicks = Math.max(0L, target.expireTick - level.getGameTime());
        target.display.setDisplay(target.multiplier, (int) remainTicks);
    }

    private static void discardDisplay(Target target) {
        if (target.display != null && !target.display.isRemoved()) {
            target.display.discard();
        }
        target.display = null;
    }

    // ------------------------------------------------------------------ 推进方块 / 生物

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean tickBlock(ServerLevel level, BlockPos pos, int multiplier) {
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
            for (int i = 0; i < multiplier; i++) {
                ticker.tick(level, pos, state, blockEntity);
            }
            return true;
        }

        if (!state.isRandomlyTicking()) {
            return false;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < multiplier; i++) {
            state.randomTick(level, pos, random);
        }
        return true;
    }

    private static void tickEntityTimers(LivingEntity entity) {
        if (entity instanceof AgeableMob ageable) {
            int age = ageable.getAge();
            if (age != 0) {
                ageable.setAge(age < 0 ? Math.min(0, age + 1) : Math.max(0, age - 1));
            }
        }

        if (entity instanceof Chicken chicken && !chicken.isBaby()) {
            chicken.eggTime = Math.max(0, chicken.eggTime - 1);
        }

        if (entity instanceof Sheep sheep && !sheep.isBaby() && sheep.isSheared()) {
            sheep.setSheared(false);
        }

        if (entity instanceof Bee bee) {
            bee.resetTicksWithoutNectarSinceExitingHive();
            bee.setStayOutOfHiveCountdown(0);
        }
    }

    private WhisperAccelerator() {
    }
}
