package io.github.crossacid.whisperway.pathway.ironguard;

import net.minecraft.util.Mth;

/**
 * 序列 9 铁卫在当前战斗中的临时状态。
 *
 * 这个状态不会写入存档。玩家死亡、退出世界或清除途径后，战势应当恢复为 0。
 *
 * @param momentum                  当前战势，范围为 0～100
 * @param lastMomentumGainGameTime  最近一次有效获得战势时的服务器游戏刻
 * @param lastMeleeAttackGameTime   最近一次有效近战攻击发生的游戏刻
 * @param meleeTargetsHitThisAttack 本次近战攻击命中的目标数量
 */
public record IronGuardCombatState(
        int momentum,
        long lastMomentumGainGameTime,
        long lastMeleeAttackGameTime,
        int meleeTargetsHitThisAttack) {

    /** 战势上限。 */
    public static final int MAX_MOMENTUM = 100;

    /** 没有获得过战势时使用的时间标记。 */
    public static final long NO_GAIN_TIME = -1L;

    /** 6 秒没有获得战势后开始衰减。Minecraft 每秒运行 20 个游戏刻。 */
    public static final int DECAY_DELAY_TICKS = 6 * 20;

    /** 战势每秒衰减一次。 */
    public static final int DECAY_INTERVAL_TICKS = 20;

    /** 每次衰减减少的战势。 */
    public static final int DECAY_AMOUNT = 5;

    /** 第一个近战目标提供8点。 */
    public static final int FIRST_MELEE_TARGET_GAIN = 8;

    /** 第二、第三个近战目标分别提供4点。 */
    public static final int ADDITIONAL_MELEE_TARGET_GAIN = 4;

    /** 同一次近战攻击最多计算3个目标。 */
    public static final int MAX_COUNTED_MELEE_TARGETS = 3;

    /** 没有战势的初始状态。 */
    public static final IronGuardCombatState EMPTY = new IronGuardCombatState(0, NO_GAIN_TIME, NO_GAIN_TIME, 0);

    /**
     * 保留两个参数构造方式，开发命令不需要修改。
     */
    public IronGuardCombatState(int momentum, long lastMomentumGainGameTime) {
        this(momentum, lastMomentumGainGameTime, NO_GAIN_TIME, 0);
    }

    public IronGuardCombatState {
        momentum = Mth.clamp(momentum, 0, MAX_MOMENTUM);
        meleeTargetsHitThisAttack = Mth.clamp(meleeTargetsHitThisAttack, 0, MAX_COUNTED_MELEE_TARGETS);

        if (momentum == 0) {
            lastMomentumGainGameTime = NO_GAIN_TIME;
        }

        if (lastMeleeAttackGameTime == NO_GAIN_TIME) {
            meleeTargetsHitThisAttack = 0;
        }
    }

    public boolean hasMomentum() {
        return momentum > 0;
    }

    public boolean canSpend(int amount) {
        return amount > 0 && momentum >= amount;
    }

    /**
     * 从格挡、开发命令等普通来源获得战势。
     */
    public IronGuardCombatState gain(int amount, long gameTime) {
        if (amount <= 0) {
            return this;
        }

        int updateMomentum = (int) Math.min(MAX_MOMENTUM, (long) momentum + amount);
        return new IronGuardCombatState(updateMomentum, gameTime, lastMeleeAttackGameTime, meleeTargetsHitThisAttack);
    }

    /**
     * 记录一次近战攻击命中的目标。
     * 同一游戏刻视为同一次近战攻击：
     * 第一个目标 +8，第二和第三个目标分别 +4，
     * 第四个及之后的目标不再增加战势。
     */
    public IronGuardCombatState gainFromMeleeTarget(long gameTime) {
        boolean sameAttack = (lastMeleeAttackGameTime == gameTime);

        int previousTargetCount = sameAttack ? meleeTargetsHitThisAttack : 0;

        if (previousTargetCount >= MAX_COUNTED_MELEE_TARGETS) {
            return this;
        }

        int gainedMomentum = previousTargetCount == 0 ? FIRST_MELEE_TARGET_GAIN : ADDITIONAL_MELEE_TARGET_GAIN;

        int updatedTargetCount = previousTargetCount + 1;

        int updatedMomentum = (int) Math.min(MAX_MOMENTUM, (long) momentum + gainedMomentum);

        return new IronGuardCombatState(updatedMomentum, gameTime, gameTime, updatedTargetCount);
    }

    public IronGuardCombatState spend(int amount) {
        if (!canSpend(amount)) {
            return this;
        }

        return new IronGuardCombatState(momentum - amount, lastMomentumGainGameTime, lastMeleeAttackGameTime,
                meleeTargetsHitThisAttack);
    }

    /**
     * 执行一次固定的自然衰减。
     * 何时调用由后续的服务器刻事件负责判断。
     */
    public IronGuardCombatState decayOnce() {
        return new IronGuardCombatState(momentum - DECAY_AMOUNT, lastMomentumGainGameTime, lastMeleeAttackGameTime,
                meleeTargetsHitThisAttack);
    }

    public IronGuardCombatState clear() {
        return EMPTY;
    }
}
