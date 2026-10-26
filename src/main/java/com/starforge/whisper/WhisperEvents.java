package com.starforge.whisper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side behaviour of the Whisper item.
 *
 * <p>被动能力（无敌 / 飞行 / 夜视 / 饱和 / 水下呼吸 / 真隐身）只要物品在背包、副手或主手任意位置
 * 就会生效；主动能力（一击必杀 / 范围攻击 / 精准 / 幸运）仍然要求拿在主手。</p>
 *
 * <p>所有效果都每 tick 从物品重新判定，没有隐藏状态：把物品丢出去或者关掉开关，效果立刻停止。</p>
 */
public final class WhisperEvents {

    /** How long the continuously refreshed effects last. Slightly more than 1 second to be safe. */
    private static final int EFFECT_DURATION = 220;

    /** 原版飞行速度基准值。 */
    private static final float BASE_FLYING_SPEED = 0.05F;

    /** 开启飞行时的倍率。 */
    private static final float FLYING_SPEED_MULTIPLIER = 4.0F;

    public static void onPlayerTick(PlayerTickEvent.Post event) {        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = WhisperData.findWhisper(player);
        if (stack.isEmpty()) {
            updateFlight(player, false);
            return;
        }

        updateFlight(player, isOn(stack, WhisperAbility.FLIGHT));

        if (isOn(stack, WhisperAbility.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.SATURATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.WATER_BREATHING)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.TRUE_INVISIBILITY)) {
            // 视觉隐身；「怪不主动锁定你」由 onLivingChangeTarget 处理
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_DURATION, 0, false, false));
        }
    }

    /** 世界 tick：驱动「加速」模式记录下来的方块与生物。 */
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WhisperAccelerator.tick(serverLevel);
        }
    }

    /** 世界加载：清掉上次残留的加速浮动文字。 */
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WhisperAccelerator.clearOrphanDisplays(serverLevel);
        }
    }

    /**
     * 基岩类方块（hardness = -1）的强制破坏。
     *
     * <p>原版 {@code getDestroyProgress} 在 hardness == -1 时直接返回 0，
     * 所以只能绕过挖掘进度：左键 START 时取消原版，服务端调用
     * {@code gameMode.destroyBlock}。这样仍然会走方块自己的破坏回调和
     * {@link BlockDropsEvent}，不会像直接 setBlockToAir 那样跳过模组逻辑。</p>
     */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof WhisperItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) >= 0.0F) {
            return;
        }

        // 客户端也取消，避免继续走原版 0 进度的挖掘；服务端负责真正破坏。
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.server.execute(() -> breakUnbreakable(serverPlayer, pos));
        }
    }

    private static void breakUnbreakable(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) >= 0.0F) {
            return;
        }

        Block block = state.getBlock();
        boolean removed = player.gameMode.destroyBlock(pos);
        if (!removed || !level.getBlockState(pos).isAir()) {
            return;
        }

        // 强挖固定补一个方块本体；onBlockDrops 对 hardness < 0 会跳过，避免双份。
        ItemStack self = new ItemStack(block);
        if (!self.isEmpty()) {
            Block.popResource(level, pos, self);
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 无敌（被动）：只要身上带着并且开关打开，就完全免疫伤害
        if (event.getEntity() instanceof ServerPlayer player
                && WhisperData.isPassiveEnabled(player, WhisperAbility.INVINCIBLE)) {
            event.setCanceled(true);
            // 氪金萝莉的写法：顺手灭火、清掉摔落距离
            player.clearFire();
            player.fallDistance = 0.0F;
        }
    }

    /**
     * 近战：一击必杀走「氪金萝莉式秒杀」，白名单目标改为固定伤害。
     *
     * <p>放在 {@link AttackEntityEvent} 里而不是伤害事件里，是因为真秒杀要绕过 {@code hurt()}，
     * 否则会被 boss 的伤害上限截断。</p>
     */
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhisperItem) || !WhisperData.isEnabled(stack, WhisperAbility.ONE_HIT)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        applyKill(player, target, player.damageSources().playerAttack(player));
        event.setCanceled(true);
    }

    /**
     * 对单个目标执行「秒杀 or 固定伤害」。
     *
     * <p>白名单（创造 / 旁观 / 持有本物品的生物）走普通伤害，其余直接设血量为 0 并调用
     * {@code die()}，这样战利品、经验、死亡动画都正常，同时绕开 boss 自定义的伤害上限。</p>
     */
    public static void applyKill(Player player, LivingEntity target, DamageSource source) {
        if (WhisperData.isProtectedTarget(target)) {
            target.hurt(source, WhisperItem.FALLBACK_DAMAGE);
            return;
        }
        target.setHealth(0.0F);
        target.die(source);
    }

    /**
     * 无敌：连死亡事件一起挡掉。
     *
     * <p>伤害已经被取消，正常情况下走不到这里；这一步是兜底（例如别的模组直接 setHealth(0)）。</p>
     */
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && WhisperData.isPassiveEnabled(player, WhisperAbility.INVINCIBLE)) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 精准模式的「强制掉落」：精准采集已经保证绝大多数方块掉本体，
     * 这里补上「正常掉落为空」的情况——直接掉方块本身（氪金萝莉的 mandatoryDrop 逻辑）。
     */
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof WhisperItem) || !WhisperData.isEnabled(tool, WhisperAbility.PRECISE)) {
            return;
        }
        // 基岩类的本体由 onLeftClickBlock 的强挖逻辑补，这里不再插一份。
        if (event.getState().getDestroySpeed(event.getLevel(), event.getPos()) < 0.0F) {
            return;
        }
        if (!event.getDrops().isEmpty()) {
            return;
        }
        ItemStack self = new ItemStack(event.getState().getBlock());
        if (self.isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, self));
    }

    /**
     * 真隐身：怪物不会把开启该功能的玩家设为目标（已经锁定过的会在下一次 AI 重选目标时放弃）。
     */
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && WhisperData.isPassiveEnabled(player, WhisperAbility.TRUE_INVISIBILITY)) {
            event.setCanceled(true);
        }
    }

    private static boolean isOn(ItemStack stack, WhisperAbility ability) {
        return WhisperData.isEnabled(stack, ability);
    }

    /**
     * Gives or removes the "may fly" ability. Creative and spectator players keep their own flight.
     *
     * <p>开启时飞行速度提升到原版的 4 倍（原版 0.05 → 0.2），参考 Re-Avaritia 的 Infinity Suit。</p>
     */
    private static void updateFlight(ServerPlayer player, boolean enabled) {
        boolean wanted = enabled || player.isCreative() || player.isSpectator();
        float wantedSpeed = enabled ? BASE_FLYING_SPEED * FLYING_SPEED_MULTIPLIER : BASE_FLYING_SPEED;

        boolean changed = false;
        if (player.getAbilities().mayfly != wanted) {
            player.getAbilities().mayfly = wanted;
            if (!wanted) {
                player.getAbilities().flying = false;
            }
            changed = true;
        }
        if (player.getAbilities().getFlyingSpeed() != wantedSpeed) {
            player.getAbilities().setFlyingSpeed(wantedSpeed);
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private WhisperEvents() {
    }
}
