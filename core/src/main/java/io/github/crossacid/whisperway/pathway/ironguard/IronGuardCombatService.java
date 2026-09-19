package io.github.crossacid.whisperway.pathway.ironguard;

import io.github.crossacid.whisperway.Whisperway;
import io.github.crossacid.whisperway.data.ModAttachments;
import io.github.crossacid.whisperway.data.PathwayProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 处理铁卫战势的服务端规则。
 */
@EventBusSubscriber(modid = Whisperway.MOD_ID)
public final class IronGuardCombatService {
    /** 近战攻击指示器至少恢复到90%。 */
    private static final float REQUIRED_ATTACK_STRENGTH = 0.9F;

    /** 远程攻击命中一个有效目标获得的战势。 */
    private static final int PROJECTILE_HIT_MOMENTUM = 2;

    /** 其他由玩家造成的间接伤害获得的战势。 */
    private static final int INDIRECT_HIT_MOMENTUM = 2;

    /** 临时战势条使用10个字符。 */
    private static final int MOMENTUM_BAR_SEGMENTS = 10;

    /** 每秒刷新一次临时战势显示。 */
    private static final int DISPLAY_REFRESH_INTERVAL_TICKS = 20;

    private IronGuardCombatService() {
    }

    /**
     * 处理最终伤害结果。
     *
     * 使用 Post 事件，是因为此时已经知道攻击是否真正造成伤害、
     * 是否被盾牌格挡，以及实际格挡了多少伤害。
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        handlePlayerCausedDamage(event);
        handleShieldBlock(event);
    }

    /**
     * 处理能够明确追溯到玩家的攻击伤害。
     */
    private static void handlePlayerCausedDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();

        // DamageSource 记录的责任人必须是玩家。
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isIronGuard(player)) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (!isValidThreat(player, target)) {
            return;
        }

        float effectiveDamage = event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION);

        if (effectiveDamage <= 0.0F) {
            return;
        }

        /*
         * 原版玩家近战伤害：
         * 直接伤害来源是玩家，并带有玩家攻击标签。
         */
        if (source.getDirectEntity() == player && source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            handleMeleeDamage(player);
            return;
        }

        /*
         * 弓箭、三叉戟以及兼容该标准的模组投射物。
         */
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
            grantMomentum(player, PROJECTILE_HIT_MOMENTUM);
            return;
        }

        /*
         * 其他能够明确追溯到玩家的伤害。
         *
         * 例如部分爆炸、魔法、荆棘或未来模组技能伤害。
         * 当前统一按较低收益处理。
         */
        grantMomentum(player, INDIRECT_HIT_MOMENTUM);
    }

    /**
     * 处理一次有效近战命中。
     */
    private static void handleMeleeDamage(ServerPlayer player) {
        /*
         * 这里检查的是攻击后的恢复进度
         * 快速连续点击产生的弱攻击不提供战势，避免通过高速点击快速刷满资源。
         */
        if (player.getAttackStrengthScale(0.5F) < REQUIRED_ATTACK_STRENGTH) {
            return;
        }

        long gameTime = player.level().getGameTime();

        IronGuardCombatState current = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);
        IronGuardCombatState updated = current.gainFromMeleeTarget(gameTime);

        if (updated == current) {
            return;
        }

        player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);
        showMomentumDisplay(player, updated);
    }

    /**
     * 使用盾牌成功格挡敌对生物攻击时获得战势。
     */
    private static void handleShieldBlock(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isIronGuard(player)) {
            return;
        }

        if (event.getBlockedDamage() <= 0.0F) {
            return;
        }

        if (!player.getUseItem().canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        if (!isValidThreat(player, attacker)) {
            return;
        }

        int gainedMomentum = getShieldBlockMomentum(event.getOriginalDamage());

        grantMomentum(player, gainedMomentum);
    }

    /**
     * 根据攻击的原始伤害计算格挡获得的战势。
     */
    private static int getShieldBlockMomentum(float originalDamage) {
        if (originalDamage < 2.0F) {
            return 0;
        }

        if (originalDamage < 6.0F) {
            return 12;
        }

        if (originalDamage < 12.0F) {
            return 16;
        }

        return 20;
    }

    /**
     * 判断一个实体是否是当前玩家的有效威胁。
     */
    private static boolean isValidThreat(ServerPlayer player, LivingEntity target) {
        if (target == player) {
            return false;
        }

        if (target instanceof Player) {
            return false;
        }

        if (target instanceof ArmorStand) {
            return false;
        }

        if (target.isInvulnerable()) {
            return false;
        }

        if (player.isAlliedTo(target)) {
            return false;
        }

        /*
         * 已驯服生物不提供战势。
         */
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }

        /*
         * Enemy 包含僵尸、骷髅、苦力怕等标准敌对生物。
         */
        if (target instanceof Enemy) {
            return true;
        }

        /*
         * 某些中立生物不实现 Enemy，
         * 但主动把玩家设为攻击目标时也算有效威胁。
         */
        return target instanceof Mob mob && mob.getTarget() == player;
    }

    /**
     * 统一的战势增加入口。
     * 未来自定义技能可以调用这个方法，按照技能自身规则提供战势。
     */
    public static void grantMomentum(ServerPlayer player, int amount) {
        if (amount <= 0 || !isIronGuard(player)) {
            return;
        }

        IronGuardCombatState current = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);
        IronGuardCombatState updated = current.gain(amount, player.level().getGameTime());

        player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);
        showMomentumDisplay(player, updated);
    }

    /**
     * 尝试消耗指定数量的战势。
     *
     * 返回 true 表示消耗成功，false 表示途径不符或战势不足。
     */
    public static boolean trySpendMomentum(ServerPlayer player, int amount) {
        if (amount <= 0 || !isIronGuard(player)) {
            return false;
        }

        IronGuardCombatState current = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);

        if (!current.canSpend(amount)) {
            return false;
        }
        IronGuardCombatState updated = current.spend(amount);
        player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);

        showMomentumDisplay(player, updated);

        return true;
    }

    /**
     * 清空玩家的铁卫战势。
     */
    public static void clearMomentum(ServerPlayer player) {
        player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, IronGuardCombatState.EMPTY);
        clearMomentumDisplay(player);
    }

    /**
     * 判断玩家当前是否为序列 9 铁卫。
     */
    private static boolean isIronGuard(ServerPlayer player) {
        PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);
        return progress.isIronGuardSequence9();
    }

    /**
     * 在快捷栏上方显示临时战势条。
     *
     * 这是开发阶段的临时界面。
     * 以后会替换为客户端自定义 HUD。
     */
    private static void showMomentumDisplay(ServerPlayer player, IronGuardCombatState state) {
        if (!state.hasMomentum()) {
            clearMomentumDisplay(player);
            return;
        }

        /*
         * 向上取整：
         * 1～10点显示1格，11～20点显示2格，以此类推。
         */
        int filledSegments = (state.momentum() * MOMENTUM_BAR_SEGMENTS + IronGuardCombatState.MAX_MOMENTUM - 1)
                / IronGuardCombatState.MAX_MOMENTUM;

        int emptySegments = MOMENTUM_BAR_SEGMENTS - filledSegments;

        String filledBar = "|".repeat(filledSegments);

        String emptyBar = "-".repeat(emptySegments);

        MutableComponent message = Component.literal("战势 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(filledBar).withStyle(ChatFormatting.RED))
                .append(Component.literal(emptyBar).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(state.momentum() + "/" + IronGuardCombatState.MAX_MOMENTUM)
                        .withStyle(ChatFormatting.WHITE));

        /*
         * 第二个参数为 true，表示显示在 Action Bar，
         * 而不是发送到聊天栏。
         */
        player.displayClientMessage(message, true);
    }

    /**
     * 清除快捷栏上方的临时战势显示。
     */
    private static void clearMomentumDisplay(ServerPlayer player) {
        player.displayClientMessage(Component.empty(), true);
    }

    /**
     * 每个游戏刻检查战势清理与衰减。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PathwayProgress progress = player.getData(ModAttachments.PATHWAY_PROGRESS);
        IronGuardCombatState state = player.getData(ModAttachments.IRON_GUARD_COMBAT_STATE);

        if (!progress.isIronGuardSequence9()) {
            if (state.hasMomentum()) {
                clearMomentum(player);
            }
            return;
        }

        if (!state.hasMomentum()) {
            return;
        }

        if (state.lastMomentumGainGameTime() == IronGuardCombatState.NO_GAIN_TIME) {
            clearMomentum(player);
            return;
        }

        long currentGameTime = player.level().getGameTime();
        long ticksSinceLastGain = currentGameTime - state.lastMomentumGainGameTime();

        /*
         * 到达衰减时间时，先更新数据，
         * 然后立即显示衰减后的数值。
         */
        if (ticksSinceLastGain >= IronGuardCombatState.DECAY_DELAY_TICKS) {
            long ticksSinceDecayStarted = ticksSinceLastGain - IronGuardCombatState.DECAY_DELAY_TICKS;
            if (ticksSinceDecayStarted % IronGuardCombatState.DECAY_INTERVAL_TICKS == 0) {
                IronGuardCombatState updated = state.decayOnce();
                player.setData(ModAttachments.IRON_GUARD_COMBAT_STATE, updated);
                showMomentumDisplay(player, updated);
                return;
            }
        }

        /*
         * Action Bar 会在一段时间后自动消失，
         * 因此有战势时每秒刷新一次。
         */
        if (currentGameTime % DISPLAY_REFRESH_INTERVAL_TICKS == 0) {
            showMomentumDisplay(player, state);
        }
    }
}
