package com.starforge.whisper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * <p>先用右击给目标挂一层 1 倍加速，再右键会翻倍（上限 {@link #MAX_MULTIPLIER} 倍），
 * 每次右键把持续时间刷新回 {@link #DURATION_TICKS}（30 秒）。</p>
 *
 * <p>方块：每 tick 按倍率驱动方块实体 ticker；没有方块实体但会随机刻的方块（作物、树苗）
 * 则按倍率执行随机刻。生物：只推进计时器（成长、繁殖冷却、下蛋、长毛、蜜蜂）。</p>
 *
 * <p>每个被加速的目标头顶挂一个 {@code TextDisplay}，第一行显示倍率，第二行显示剩余秒数。
 * 状态只存在内存里，重进世界会清空。</p>
 */
public final class WhisperAccelerator {

    /** 加速持续 30 秒。 */
    public static final int DURATION_TICKS = 600;

    /** 倍率上限。 */
    public static final int MAX_MULTIPLIER = 64;

    /** 每秒刷新一次头顶文字。 */
    private static final int DISPLAY_INTERVAL = 20;

    /** 打在浮动文字实体上的标记，用于重启后清理残留。 */
    private static final String ORPHAN_TAG = "starforge_accel_display";

    private static final Map<ResourceKey<Level>, Map<BlockPos, Target>> BLOCKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, Target>> ENTITIES = new HashMap<>();

    /** 单个被加速目标的运行时状态。 */
    private static final class Target {
        int multiplier = 1;
        long expireTick;
        Display.TextDisplay display;
    }

    public static void accelerateBlock(ServerLevel level, BlockPos pos) {
        BlockPos key = pos.immutable();
        Map<BlockPos, Target> map = BLOCKS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Target target = map.get(key);
        boolean fresh = target == null;
        if (fresh) {
            target = new Target();
            map.put(key, target);
            target.display = spawnDisplay(level, Vec3.atCenterOf(key).add(0.0D, 1.3D, 0.0D));
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
            target.display = spawnDisplay(level, displayPosFor(entity));
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
                if (target.display != null && !target.display.isRemoved()) {
                    target.display.setPos(displayPosFor(living));
                }
                if (now % DISPLAY_INTERVAL == 0) {
                    updateDisplay(level, target);
                }
            }
        }
    }

    // ------------------------------------------------------------------ 头顶文字

    private static Vec3 displayPosFor(LivingEntity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.8D, entity.getZ());
    }

    private static Display.TextDisplay spawnDisplay(ServerLevel level, Vec3 pos) {
        Display.TextDisplay display = EntityType.TEXT_DISPLAY.create(level);
        if (display == null) {
            return null;
        }
        display.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        display.setNoGravity(true);
        display.addTag(ORPHAN_TAG);
        level.addFreshEntity(display);
        return display;
    }

    /**
     * 世界加载时清掉上一次残留的浮动文字。
     *
     * <p>加速状态只存在内存里，重启后目标列表是空的，但 {@code TextDisplay} 是真实实体、
     * 会被存档保存下来，所以如果不清理，重进世界会看到一堆永远的「x64 30s」。</p>
     */
    public static void clearOrphanDisplays(ServerLevel level) {
        java.util.List<Entity> orphans = new java.util.ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Display.TextDisplay && entity.getTags().contains(ORPHAN_TAG)) {
                orphans.add(entity);
            }
        }
        for (Entity orphan : orphans) {
            orphan.discard();
        }
    }

    /**
     * 更新头顶文字：第一行倍率，第二行剩余秒数。
     *
     * <p>1.21 里 {@code TextDisplay} 的 setter 全是 private，只能通过 NBT（{@code load}）写数据。</p>
     */
    private static void updateDisplay(ServerLevel level, Target target) {
        if (target.display == null || target.display.isRemoved()) {
            return;
        }
        long remainTicks = Math.max(0L, target.expireTick - level.getGameTime());
        String text = "x" + target.multiplier + "\n" + (remainTicks / 20L) + "s";

        CompoundTag tag = new CompoundTag();
        tag.putString("text", Component.Serializer.toJson(Component.literal(text), level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putInt("background", 0x40000000);
        tag.putBoolean("see_through", true);
        target.display.load(tag);
    }

    private static void discardDisplay(Target target) {
        if (target.display != null && !target.display.isRemoved()) {
            target.display.discard();
        }
        target.display = null;
    }

    // ------------------------------------------------------------------ 推进方块 / 生物

    /**
     * 推进一个方块 {@code multiplier} 次。返回 {@code false} 表示这个位置已经不值得继续加速。
     */
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

    /** 只推进生物身上的计时器，不跑 AI。 */
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
