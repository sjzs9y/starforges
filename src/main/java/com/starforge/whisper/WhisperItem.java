package com.starforge.whisper;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 轻语轻扬。
 *
 * <p>它同时是一把「万能工具」：{@link #getDestroySpeed} 与 {@link #isCorrectToolForDrops} 被改写，
 * 所以任何方块都能挖、而且必定正常掉落（基岩/屏障这类硬度 -1 的方块除外，原版逻辑本来就不允许破坏）。</p>
 *
 * <p>精准模式 / 幸运模式不是自己算掉落，而是给物品本身挂上真实的附魔：
 * 精准 = 精准采集 I，幸运 = 时运 20 + 抢夺 20。这样原版与其它模组的掉落逻辑都会自动认账。</p>
 */
public class WhisperItem extends Item {

    /** 幸运模式下的时运等级。 */
    public static final int FORTUNE_LEVEL = 20;
    /** 幸运模式下的抢夺等级。 */
    public static final int LOOTING_LEVEL = 20;

    /** 基础挖掘速度；实际返回值会再乘上方块硬度。 */
    private static final float BASE_MINING_SPEED = 12.0F;

    public WhisperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.starforge.whisper.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.starforge.whisper.attack").withStyle(ChatFormatting.GRAY));
    }

    /** 右键范围攻击的半径：50 格，即 100×100×100 的范围。 */
    public static final double ATTACK_RADIUS = 50.0D;

    /** 范围攻击冷却：20 tick = 1 秒。 */
    public static final int ATTACK_COOLDOWN_TICKS = 20;

    /** 白名单目标（创造 / 旁观 / 持有本物品的生物）挨的固定伤害，而不是被秒杀。 */
    public static final float FALLBACK_DAMAGE = 10.0F;

    /**
     * 右键触发范围攻击。
     *
     * <p>返回 {@code consume} 并进入「使用中」状态，这样按住右键时 {@link #onUseTick} 会每 tick 回调；
     * 配合 {@link #getUseDuration} 就能实现「按住持续清场」。</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WhisperData.isEnabled(stack, WhisperAbility.ONE_HIT)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            attackArea(level, player);
            player.getCooldowns().addCooldown(stack.getItem(), ATTACK_COOLDOWN_TICKS);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** 按住右键时每 tick 回调；冷却好了就再清一次，所以按住等于每秒一次。 */
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(living instanceof Player player)) {
            return;
        }
        if (!WhisperData.isEnabled(stack, WhisperAbility.ONE_HIT)) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        attackArea(level, player);
        player.getCooldowns().addCooldown(stack.getItem(), ATTACK_COOLDOWN_TICKS);
    }

    /** 「使用中」的持续时间；给一个很大的值，松手才结束。 */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /**
     * 对周围 100×100×100 范围内的生物执行秒杀（白名单目标改为固定伤害），
     * 并清掉「开打之前就已经在地上」的掉落物和经验球——新掉出来的战利品会保留。
     */
    private static void attackArea(Level level, Player player) {
        DamageSource source = player.damageSources().playerAttack(player);
        AABB area = player.getBoundingBox().inflate(ATTACK_RADIUS);

        // 先快照：只清理这一轮攻击之前就存在的掉落物，新掉落保留
        List<Entity> staleLoot = new ArrayList<>();
        staleLoot.addAll(level.getEntitiesOfClass(ItemEntity.class, area));
        staleLoot.addAll(level.getEntitiesOfClass(ExperienceOrb.class, area));

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player || !target.isAlive()) {
                continue;
            }
            WhisperEvents.applyKill(player, target, source);
        }

        for (Entity entity : staleLoot) {
            entity.discard();
        }
    }

    /**
     * 挖掘速度 = 基础速度 × 硬度，因此耗时恒定（约 2~3 tick），挖什么都是一样快。
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float hardness = state.getDestroySpeed(null, null);
        if (hardness < 0.0F) {
            // 硬度 -1 是「伪不可破坏」的约定值，原版 BlockBehaviour 会直接让进度归零。
            // 这里给一个正数，既不会变成 0 导致除零，也不会让基岩真的被挖掉。
            return BASE_MINING_SPEED * 10.0F;
        }
        return Math.max(BASE_MINING_SPEED, BASE_MINING_SPEED * hardness);
    }

    /** 永远判定为「正确的工具」，所以任何方块被破坏都会正常掉落。 */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    /**
     * 物品带在身上时每 tick 调用；用来让附魔与当前模式保持一致。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide()) {
            return;
        }
        syncEnchantments(stack, level);
    }

    /**
     * 精准模式 = 精准采集 I；幸运模式 = 时运 20 + 抢夺 20；两个都关 = 无附魔。
     * 两个模式互斥由 {@link WhisperAbility#getExclusive()} 与网络包处理，这里只负责反映结果。
     */
    private static void syncEnchantments(ItemStack stack, Level level) {
        boolean precise = WhisperData.isEnabled(stack, WhisperAbility.PRECISE);
        boolean lucky = WhisperData.isEnabled(stack, WhisperAbility.LUCKY);

        HolderLookup.Provider lookup = level.registryAccess();
        setEnchantment(stack, lookup, Enchantments.SILK_TOUCH, precise ? 1 : 0);
        setEnchantment(stack, lookup, Enchantments.FORTUNE, lucky ? FORTUNE_LEVEL : 0);
        setEnchantment(stack, lookup, Enchantments.LOOTING, lucky ? LOOTING_LEVEL : 0);

        // 关掉附魔光效，保持「普普通通」的外观
        if (!Boolean.FALSE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.FALSE);
        }
    }

    private static void setEnchantment(ItemStack stack, HolderLookup.Provider lookup,
                                       ResourceKey<Enchantment> key, int level) {
        Holder.Reference<Enchantment> holder = lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (current.getLevel(holder) == level) {
            return;
        }
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(holder, level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }
}
